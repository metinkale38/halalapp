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