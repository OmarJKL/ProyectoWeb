document.addEventListener('DOMContentLoaded', () => {
    const btnLogout = document.getElementById('btnCerrarSesion');
    if (btnLogout) {
        btnLogout.addEventListener('click', () => {
            window.location.href = '/logout';
        });
    }
});
