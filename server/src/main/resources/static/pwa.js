if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
        navigator.serviceWorker.register('/sw.js')
            .then((reg) => console.log('Service Worker registriert.', reg))
            .catch((err) => console.log('Service Worker Fehler:', err));
    });
}


let deferredPrompt;

window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    deferredPrompt = e;

    if (window.innerWidth < 769) {
        document.getElementById('installBtn').style.display = 'block';
    }
});

function installApp() {
    document.getElementById('installBtn').style.display = 'none';
    deferredPrompt.prompt();

    deferredPrompt.userChoice.then((choiceResult) => {
        if (choiceResult.outcome === 'accepted') {
            console.log('Nutzer hat die App installiert.');
        } else {
            console.log('Installation abgebrochen.');
        }
        deferredPrompt = null;
    });
}

window.addEventListener('appinstalled', (evt) => {
    document.getElementById('installBtn').style.display = 'none';
    console.log('PWA wurde erfolgreich installiert.');
});