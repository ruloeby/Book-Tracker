// فتح وإغلاق login/register modal
document.addEventListener('DOMContentLoaded', () => {
    const loginOverlay = document.getElementById('loginOverlay');
    const registerOverlay = document.getElementById('registerOverlay');

    // فتح الفورم
    const openLogin = document.getElementById('openLogin');
    const openRegister = document.getElementById('openRegister');

    const closeLogin = document.getElementById('closeLogin');
    const closeRegister = document.getElementById('closeRegister');

    if(openLogin){
        openLogin.addEventListener('click', (e) => {
            e.preventDefault();
            loginOverlay.style.display = 'flex';
            registerOverlay.style.display = 'none';
        });
    }

    if(openRegister){
        openRegister.addEventListener('click', (e) => {
            e.preventDefault();
            registerOverlay.style.display = 'flex';
            loginOverlay.style.display = 'none';
        });
    }

    if(closeLogin){
        closeLogin.addEventListener('click', () => loginOverlay.style.display = 'none');
    }

    if(closeRegister){
        closeRegister.addEventListener('click', () => registerOverlay.style.display = 'none');
    }

    // اغلاق عند الضغط خارج الفورم
    if(loginOverlay){
        loginOverlay.addEventListener('click', e => {
            if(e.target === loginOverlay) loginOverlay.style.display = 'none';
        });
    }
    if(registerOverlay){
        registerOverlay.addEventListener('click', e => {
            if(e.target === registerOverlay) registerOverlay.style.display = 'none';
        });
    }
});
