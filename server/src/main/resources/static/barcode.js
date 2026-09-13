window.addEventListener('load', () => {
    const scanner = new Html5QrcodeScanner(
        "scanner",
        {
            fps: 10,
            qrbox: { width: 250, height: 250 },
            aspectRatio: 1.0,
            rememberLastUsedCamera: true,
            showTorchButtonIfSupported: true
        },
        false
    );

    scanner.render((decodedText, decodedResult) => {
        scanner.clear().then(() => {
            window.location.href = `/product/${encodeURIComponent(decodedText)}`;
        });
    }, (errorMessage) => {
    });
});