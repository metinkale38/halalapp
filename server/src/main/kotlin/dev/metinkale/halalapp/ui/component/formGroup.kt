package dev.metinkale.halalapp.html

import kotlinx.html.*


fun FlowContent.formGroup(block: FormGroupContent.() -> Unit = {}) {
    div("flex flex-col gap-4") {
        object : FormGroupContent, FlowContent by this {
            init {
                block()
            }
        }
    }
}

interface FormGroupContent : FlowContent {

    fun staticField(label: String, name: String, value: String) {
        label {
            span(classes = "text-sm font-medium text-gray-700 mb-1") { +label }
            input(
                classes =
                    "w-full rounded-md border-gray-300 shadow-sm text-gray-500 focus:ring-0"
            ) {
                this.name = name
                this.value = value
                readonly = true
            }
        }
    }


    fun inputField(label: String, name: String, value: String? = null, block: INPUT.() -> Unit = {}) {
        label {
            span(classes = "text-sm font-medium text-gray-700 mb-1") { +label }
            input(
                classes =
                    "w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
            ) {
                this.name = name
                value?.let { this.value = it }
                block()
            }
        }
    }

    fun checkBoxField(label: String, name: String, checked: Boolean = false, block: INPUT.() -> Unit = {}) {
        label("flex") {
            span(classes = "text-sm font-medium text-gray-700 grow") { +label }
            input(
                type = InputType.checkBox,
                classes = "rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
            ) {
                this.name = name
                this.checked = checked
                this.value = "true"
                this.onChange = "this.value = this.checked ? 'true' : 'false';"
                block()
            }
        }
    }

    fun textAreaField(label: String, name: String, value: String? = null, block: TEXTAREA.() -> Unit = {}) {
        label {
            span(classes = "text-sm font-medium text-gray-700 mb-1") { +label }
            textArea(
                classes =
                    "w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
            ) {
                this.name = name
                rows = "7"
                block()
                value?.let { +it }
            }
        }
    }

    fun selectField(
        label: String,
        name: String,
        options: List<Pair<String, String>>,
        selected: String? = null,
        block: SELECT.() -> Unit = {},
    ) {
        label {
            span(classes = "text-sm font-medium text-gray-700 mb-1") { +label }
            select(
                classes =
                    "w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
            ) {
                this.name = name
                options.forEach { (key, value) ->
                    option {
                        this.value = key
                        if (key == selected) this.selected = true
                        +value
                    }
                }
                block()
            }
        }
    }

    fun submitButton(text: String = "Absenden") {
        input(
            type = InputType.submit,
            classes = "px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 cursor-pointer col-start-2 max-w-[14rem]"
        ) {
            value = text
        }
    }

    fun multiSelect(
        label: String, name: String, options: List<Pair<String, String>>, selectedList: List<String>,
        block: SELECT.() -> Unit = {},
    ) {
        label {
            span(classes = "text-sm font-medium text-gray-700 mb-1 group") { +label }
            div("flex flex-col gap-2 mb-2") {
                for (selected in selectedList) {
                    div("flex flex-row items-center gap-4 [&:only-child>a]:hidden") {
                        select(
                            classes =
                                "w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 grow"
                        ) {
                            this.name = name
                            options.forEach { (key, value) ->
                                option {
                                    this.value = key
                                    if (key == selected) this.selected = true
                                    +value
                                }
                            }
                            block()
                        }
                        a(classes = "px-3 py-1 text-xs font-semibold bg-red-50 text-red-600 rounded-md hover:bg-red-100 transition-colors") {
                            onClick = "this.parentNode.remove();"
                            +"Löschen"
                        }
                    }
                }
            }
            a (classes="px-3 py-1 text-xs font-semibold bg-blue-50 text-blue-700 rounded-md hover:bg-blue-100 transition-colors"){
                onClick="this.previousElementSibling.appendChild(this.previousElementSibling.lastElementChild.cloneNode(true))"
                +"Weitere hinzufügen"
            }

        }
    }

}