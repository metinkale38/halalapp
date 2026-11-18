package dev.metinkale.halalapp.ui.component

import kotlinx.html.*

fun FlowContent.photoField(
    label: String,
    name: String,
    previewId: String,
    existingUrl: String?,
    removalName: String? = null,
) {
    div(classes = "flex flex-col gap-2") {
        span(classes = "text-sm font-medium text-gray-700") { +label }
        input(classes = "w-full rounded-md border-gray-300 shadow-sm text-sm") {
            type = InputType.file
            this.name = name
            accept = "image/jpeg,image/png"
            onChange = "normalizePhoto(this, '$previewId')"
        }
        if (existingUrl != null) {
            img(classes = "max-h-48 w-auto rounded-lg border border-gray-200 object-contain") {
                src = existingUrl
                alt = label
            }
            if (removalName != null) {
                label(classes = "flex items-center gap-2 text-sm text-red-700") {
                    input(type = InputType.checkBox, classes = "rounded border-gray-300 text-red-600 focus:ring-red-500") {
                        this.name = removalName
                        value = "true"
                    }
                    +"Vorhandenes Bild entfernen"
                }
            }
        }
        img(classes = "hidden max-h-48 w-auto rounded-lg border border-gray-200 object-contain") {
            id = previewId
            alt = "Vorschau"
        }
    }
}

fun HEAD.loadPhotoNormalizer() {
    script {
        unsafe {
            +"""
                async function normalizePhoto(input, previewId) {
                    const file = input.files && input.files[0];
                    const preview = document.getElementById(previewId);
                    if (!file || !preview) return;
                    preview.src = URL.createObjectURL(file);
                    preview.classList.remove('hidden');
                    try {
                        const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
                        const max = 2000;
                        let width = bitmap.width;
                        let height = bitmap.height;
                        if (width > max || height > max) {
                            const factor = Math.min(max / width, max / height);
                            width = Math.round(width * factor);
                            height = Math.round(height * factor);
                        }
                        const canvas = document.createElement('canvas');
                        canvas.width = width;
                        canvas.height = height;
                        canvas.getContext('2d').drawImage(bitmap, 0, 0, width, height);
                        const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.9));
                        if (!blob) return;
                        const parts = file.name.split('.');
                        parts.pop();
                        const name = (parts.join('.') || 'foto') + '.jpg';
                        const transfer = new DataTransfer();
                        transfer.items.add(new File([blob], name, { type: 'image/jpeg' }));
                        input.files = transfer.files;
                        preview.src = URL.createObjectURL(blob);
                    } catch (e) {
                        console.warn('Bild konnte nicht optimiert werden', e);
                    }
                }
            """.trimIndent()
        }
    }
}
