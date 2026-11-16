(function () {
    const scannerElement = document.getElementById("scanner");
    if (!scannerElement) return;

    const statusElement = document.getElementById("scanner-status");
    const statusTextElement = document.getElementById("scanner-status-text");
    const errorElement = document.getElementById("scanner-error");
    const errorTextElement = document.getElementById("scanner-error-text");
    const retryButton = document.getElementById("scanner-retry");
    const torchButton = document.getElementById("torch-button");
    const switchCameraButton = document.getElementById("switch-camera-button");
    const hintElement = document.getElementById("scanner-hint");

    const supportedFormats = window.Html5QrcodeSupportedFormats
        ? [
            Html5QrcodeSupportedFormats.EAN_13,
            Html5QrcodeSupportedFormats.EAN_8,
            Html5QrcodeSupportedFormats.UPC_A,
            Html5QrcodeSupportedFormats.UPC_E,
            Html5QrcodeSupportedFormats.CODE_128,
            Html5QrcodeSupportedFormats.CODE_39,
            Html5QrcodeSupportedFormats.ITF,
            Html5QrcodeSupportedFormats.QR_CODE,
        ]
        : undefined;

    const scanConfig = {
        fps: 10,
        disableFlip: false,
        ...(supportedFormats ? { formatsToSupport: supportedFormats } : {}),
    };

    const scannerOptions = {
        verbose: false,
        ...(supportedFormats ? { formatsToSupport: supportedFormats } : {}),
    };

    let scanner = null;
    let cameras = [];
    let currentFacing = "environment";
    let torchEnabled = false;
    let torchSupported = false;
    let switching = false;
    let handlingResult = false;
    let hintTimeout = null;
    let wakeLock = null;
    let pausedByVisibility = false;
    let stopForVisibility = Promise.resolve();

    function isBackCamera(camera) {
        return /(back|rear|environment|hinten|rück)/i.test(camera.label || "");
    }

    function isFrontCamera(camera) {
        return /(front|user|facing user|face|vorder|selbst)/i.test(camera.label || "");
    }

    function setStatus(text) {
        if (!statusElement || !statusTextElement) return;
        if (text) {
            statusTextElement.textContent = text;
            statusElement.style.display = "flex";
        } else {
            statusElement.style.display = "none";
        }
    }

    function showHint() {
        if (!hintElement) return;
        clearTimeout(hintTimeout);
        hintElement.style.display = "block";
        hintTimeout = setTimeout(function () {
            hintElement.style.display = "none";
        }, 4000);
    }

    async function requestWakeLock() {
        if (!navigator.wakeLock || document.visibilityState !== "visible" || wakeLock) return;

        try {
            wakeLock = await navigator.wakeLock.request("screen");
            wakeLock.addEventListener("release", function () {
                wakeLock = null;
            });
        } catch (error) {
            wakeLock = null;
        }
    }

    async function releaseWakeLock() {
        if (!wakeLock) return;

        const lock = wakeLock;
        wakeLock = null;
        try {
            await lock.release();
        } catch (error) {
        }
    }

    function showError(text) {
        setStatus(null);
        if (hintElement) hintElement.style.display = "none";
        if (errorTextElement) errorTextElement.textContent = text;
        if (errorElement) errorElement.style.display = "flex";
        releaseWakeLock();
        resetControls();
    }

    function hideError() {
        if (errorElement) errorElement.style.display = "none";
    }

    function resetControls() {
        torchEnabled = false;
        torchSupported = false;
        if (torchButton) {
            torchButton.classList.add("hidden");
            torchButton.setAttribute("aria-pressed", "false");
        }
        if (switchCameraButton) {
            switchCameraButton.classList.add("hidden");
        }
    }

    function getScanner() {
        if (!scanner) {
            scanner = new Html5Qrcode("scanner", scannerOptions);
        }
        return scanner;
    }

    async function refreshCameras() {
        if (!navigator.mediaDevices || !navigator.mediaDevices.enumerateDevices) {
            cameras = [];
            return;
        }

        try {
            const devices = await navigator.mediaDevices.enumerateDevices();
            cameras = devices
                .filter(function (device) {
                    return device.kind === "videoinput";
                })
                .map(function (device) {
                    return { id: device.deviceId, label: device.label };
                });
        } catch (error) {
            cameras = [];
        }
    }

    function updateSwitchCameraButton() {
        if (!switchCameraButton) return;
        switchCameraButton.classList.toggle("hidden", cameras.length <= 1);
    }

    async function updateTorchButton() {
        if (!scanner || !scanner.isScanning) return;

        try {
            const capabilities = scanner.getRunningTrackCapabilities();
            torchSupported = !!(capabilities && capabilities.torch);
        } catch (error) {
            torchSupported = false;
        }

        if (!torchButton) return;
        torchButton.classList.toggle("hidden", !torchSupported);
        if (!torchSupported) {
            torchEnabled = false;
            torchButton.setAttribute("aria-pressed", "false");
        }
    }

    async function onCameraStarted() {
        setStatus(null);

        if (document.hidden) {
            pausedByVisibility = true;
            handlingResult = false;
            stopForVisibility = scanner.stop().catch(function () {});
            return;
        }

        showHint();

        await refreshCameras();
        updateSwitchCameraButton();

        try {
            const settings = scanner.getRunningTrackSettings();
            currentFacing = settings.facingMode || "environment";
        } catch (error) {
            currentFacing = "environment";
        }

        await updateTorchButton();
        await requestWakeLock();
    }

    function showStartError(error) {
        const details = [
            error && error.name,
            error && error.message,
            String(error),
        ].filter(Boolean).join(" ");

        let message = "Die Kamera konnte nicht gestartet werden. Bitte versuche es erneut.";

        if (/NotAllowedError|Permission|denied/i.test(details)) {
            message = "Kamerazugriff wurde verweigert. Bitte erlaube den Zugriff in den Browsereinstellungen und versuche es erneut.";
        } else if (/NotFoundError|OverconstrainedError|DevicesNotFoundError/i.test(details)) {
            message = "Es wurde keine passende Kamera gefunden.";
        } else if (/NotReadableError|TrackStartError|AbortError/i.test(details)) {
            message = "Die Kamera wird bereits von einer anderen Anwendung verwendet.";
        } else if (/NotSupportedError|SecurityError/i.test(details)) {
            message = "Der Kamerazugriff ist in diesem Browser oder Kontext nicht erlaubt. Bitte verwende HTTPS.";
        }

        console.error("Barcode-Scanner konnte nicht gestartet werden:", error);
        showError(message);
    }

    async function startCamera(constraints) {
        setStatus("Kamera wird gestartet…");

        try {
            const html5Qrcode = getScanner();
            if (html5Qrcode.isScanning) {
                await html5Qrcode.stop();
            }
            await html5Qrcode.start(
                constraints,
                scanConfig,
                onDetection,
                function () {}
            );
        } catch (error) {
            showStartError(error);
            return;
        }

        await onCameraStarted();
    }

    async function startInitialCamera() {
        if (switching) return;

        handlingResult = false;
        pausedByVisibility = false;
        hideError();
        resetControls();

        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            showError("Dein Browser unterstützt keinen Kamerazugriff. Bitte verwende eine aktuelle Version über HTTPS.");
            return;
        }

        await startCamera({ facingMode: "environment" });
    }

    async function resumeCameraAfterVisibility() {
        if (!pausedByVisibility) return;

        pausedByVisibility = false;
        handlingResult = false;
        await stopForVisibility;
        hideError();
        resetControls();

        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            return;
        }

        await startCamera({ facingMode: currentFacing });
    }

    async function toggleTorch() {
        if (!scanner || !torchSupported || !torchButton) return;

        const nextState = !torchEnabled;
        torchButton.disabled = true;

        try {
            await scanner.applyVideoConstraints({ advanced: [{ torch: nextState }] });
            torchEnabled = nextState;
            torchButton.setAttribute("aria-pressed", String(nextState));
        } catch (error) {
            console.warn("Blitzlicht konnte nicht umgeschaltet werden:", error);
            torchSupported = false;
            torchEnabled = false;
            torchButton.classList.add("hidden");
            torchButton.setAttribute("aria-pressed", "false");
        } finally {
            torchButton.disabled = false;
        }
    }

    async function switchCamera() {
        if (switching || !scanner || !scanner.isScanning) return;

        switching = true;
        if (switchCameraButton) switchCameraButton.disabled = true;

        const targetFacing = currentFacing === "environment" ? "user" : "environment";
        let target = { facingMode: targetFacing };

        if (cameras.length > 1) {
            const match = cameras.find(function (camera) {
                return targetFacing === "environment" ? isBackCamera(camera) : isFrontCamera(camera);
            });
            if (match) target = match.id;
        }

        torchEnabled = false;
        if (torchButton) torchButton.classList.add("hidden");

        try {
            await scanner.stop();
            await scanner.start(target, scanConfig, onDetection, function () {});

            try {
                const settings = scanner.getRunningTrackSettings();
                currentFacing = settings.facingMode || targetFacing;
            } catch (error) {
                currentFacing = targetFacing;
            }

            await updateTorchButton();

            if (document.hidden) {
                pausedByVisibility = true;
                handlingResult = false;
                stopForVisibility = scanner.stop().catch(function () {});
            }
        } catch (error) {
            showStartError(error);
        } finally {
            switching = false;
            if (switchCameraButton) switchCameraButton.disabled = false;
        }
    }

    function onDetection(decodedText) {
        if (handlingResult || switching) return;
        handlingResult = true;

        if (navigator.vibrate) navigator.vibrate(80);
        setStatus("Barcode erkannt…");
        releaseWakeLock();

        const target = "/product/" + encodeURIComponent(decodedText);
        const navigate = function () {
            window.location.href = target;
        };

        if (scanner && scanner.isScanning) {
            scanner.stop().then(navigate).catch(navigate);
        } else {
            navigate();
        }
    }

    if (torchButton) {
        torchButton.addEventListener("click", toggleTorch);
    }
    if (switchCameraButton) {
        switchCameraButton.addEventListener("click", switchCamera);
    }
    if (retryButton) {
        retryButton.addEventListener("click", startInitialCamera);
    }

    document.addEventListener("visibilitychange", function () {
        if (document.hidden) {
            if (scanner && scanner.isScanning && !switching) {
                pausedByVisibility = true;
                handlingResult = false;
                releaseWakeLock();
                stopForVisibility = scanner.stop().catch(function () {});
            }
        } else {
            resumeCameraAfterVisibility();
        }
    });

    startInitialCamera();
})();
