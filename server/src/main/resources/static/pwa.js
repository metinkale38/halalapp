if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
        navigator.serviceWorker.register('/sw.js')
            .then((reg) => console.log('Service Worker registriert.', reg))
            .catch((err) => console.log('Service Worker Fehler:', err));
    });
}


let deferredPrompt;

const installBtn = () => document.getElementById('installBtn');
const installHint = () => document.getElementById('installHint');

const isIos = () =>
    /iPad|iPhone|iPod/.test(navigator.userAgent) ||
    (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);

const isStandalone = () =>
    window.matchMedia('(display-mode: standalone)').matches ||
    navigator.standalone === true;

const isMobile = () => window.innerWidth < 769;

const showInstallButton = () => {
    const btn = installBtn();
    if (btn) {
        btn.style.display = 'block';
    }
};

window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    deferredPrompt = e;

    if (isMobile()) {
        showInstallButton();
    }
});

if (isIos() && !isStandalone()) {
    showInstallButton();
}

function installApp() {
    const btn = installBtn();
    const hint = installHint();

    if (deferredPrompt) {
        if (btn) btn.style.display = 'none';
        deferredPrompt.prompt();

        deferredPrompt.userChoice.then((choiceResult) => {
            if (choiceResult.outcome === 'accepted') {
                console.log('Nutzer hat die App installiert.');
            } else {
                console.log('Installation abgebrochen.');
            }
            deferredPrompt = null;
        });
        return;
    }

    if (isIos() && hint) {
        hint.classList.toggle('hidden');
        hint.classList.toggle('flex');
        hint.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
}

window.addEventListener('appinstalled', (evt) => {
    const btn = installBtn();
    if (btn) btn.style.display = 'none';
    console.log('PWA wurde erfolgreich installiert.');
});
