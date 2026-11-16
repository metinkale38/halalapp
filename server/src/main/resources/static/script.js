function isSharingSupported() {
    if (window.AndroidShare) {
        return window.AndroidShare.isSharingSupported();
    }
    return !!(navigator.share);
}

async function shareContent(title, text, url) {
    if (window.AndroidShare) {
        try {
            window.AndroidShare.shareContent(title || "", text || "", url || "");
            console.log("Erfolgreich geteilt!");
        } catch (err) {
            console.error("Fehler beim Teilen:", err);
        }
        return;
    }
    if (!isSharingSupported()) {
        console.warn("Web Share API wird nicht unterstützt.");
        return;
    }

    try {
        await navigator.share({
            title: title,
            text: text,
            url: url,
        });
        console.log("Erfolgreich geteilt!");
    } catch (err) {
        console.error("Fehler beim Teilen:", err);
    }
}


var nav = undefined;
window.addEventListener('scroll', function () {
    if (!nav) nav = document.querySelector('nav')
    if (!nav) return;
    if (window.scrollY > 10) {
        nav.classList.add('shadow-sm');
    } else {
        nav.classList.remove('shadow-sm');
    }
});


function sendMail(e, subject, content = "") {
    if (!e || !e.isTrusted) return;
    e.preventDefault();

    let host = window.location.host.split(':')[0];
    const domainMatch = host.match(/([^.]+\.[^.]+)$/);
    const domain = domainMatch ? domainMatch[0] : host;

    const user = "info";
    let mailtoLink = "mai" + "lto" + `:${user}@${domain}?subject=${encodeURIComponent(subject)}`;

    if (content) {
        mailtoLink += `&body=${encodeURIComponent(content)}`;
    }

    window.location.href = mailtoLink;
}

function showCrmToast(message, isError) {
    let toast = document.getElementById('crm-toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'crm-toast';
        toast.style.cssText = 'position:fixed;left:50%;bottom:24px;transform:translateX(-50%);z-index:9999;padding:12px 20px;border-radius:10px;color:#fff;font:500 14px system-ui,sans-serif;box-shadow:0 10px 25px rgba(0,0,0,.2);transition:opacity .25s;max-width:calc(100vw - 32px);text-align:center;';
        document.body.appendChild(toast);
    }
    toast.textContent = message;
    toast.style.background = isError ? '#b91c1c' : '#047857';
    toast.style.opacity = '1';
    clearTimeout(window.crmToastTimer);
    window.crmToastTimer = setTimeout(function () {
        toast.style.opacity = '0';
    }, 2800);
}

function refreshCrmEmptyState() {
    const cards = document.querySelectorAll('[data-crm-card]');
    const empty = document.querySelector('[data-crm-empty]');
    if (empty) empty.classList.toggle('hidden', cards.length > 0);
}

function initCrmActions() {
    document.querySelectorAll('form[data-crm-action]').forEach(function (form) {
        form.addEventListener('submit', async function (event) {
            event.preventDefault();

            const confirmText = form.dataset.crmConfirm;
            if (confirmText && !window.confirm(confirmText)) return;

            const card = form.closest('[data-crm-card]');
            const buttons = Array.from(form.querySelectorAll('button,input[type=submit]'));
            const original = buttons.map(function (button) { return button.value; });
            buttons.forEach(function (button) {
                button.disabled = true;
                button.value = 'Wird gespeichert…';
            });

            try {
                const response = await fetch(form.action, {
                    method: 'POST',
                    body: new FormData(form),
                    credentials: 'same-origin',
                    headers: {'X-Requested-With': 'fetch'}
                });

                if (!response.ok) {
                    const text = (await response.text()).trim();
                    throw new Error(text || 'Aktion fehlgeschlagen.');
                }

                if (card) {
                    card.style.transition = 'opacity .2s';
                    card.style.opacity = '0';
                    setTimeout(function () {
                        card.remove();
                        refreshCrmEmptyState();
                    }, 200);
                } else {
                    refreshCrmEmptyState();
                }
                showCrmToast(form.dataset.crmSuccess || 'Erledigt.');
            } catch (error) {
                showCrmToast(error.message || 'Aktion fehlgeschlagen.', true);
                buttons.forEach(function (button, index) {
                    button.disabled = false;
                    button.value = original[index];
                });
            }
        });
    });
    refreshCrmEmptyState();
}

document.addEventListener('DOMContentLoaded', initCrmActions);
