
const authForm = document.getElementById('authForm');
const loginTab = document.getElementById('loginTab');
const registerTab = document.getElementById('registerTab');
const confirmGroup = document.getElementById('confirmGroup');
const confirmPassword = document.getElementById('confirmPassword');
const passwordInput = document.getElementById('password');
const authMessage = document.getElementById('authMessage');
const submitButton = document.getElementById('submitButton');
const formTitle = document.getElementById('formTitle');
const formSubtitle = document.getElementById('formSubtitle');
const switchButton = document.getElementById('switchButton');
const switchCopy = document.getElementById('switchCopy');

let currentMode = 'login';

// API request function
async function sendRequest(url, options = {}) {
    const response = await fetch(url, {
        credentials: 'same-origin',
        headers: {
            'Content-Type': 'application/json'
        },
        ...options
    });

    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
        throw new Error(
            data.error || data.message || 'Something went wrong'
        );
    }

    return data;
}

// Display messages
function showMessage(message, type = '') {
    authMessage.textContent = message;
    authMessage.className = `form-message ${type}`;
}

// Switch between Login and Register
function setMode(mode) {
    currentMode = mode;

    const registering = mode === 'register';

    loginTab.classList.toggle('active', !registering);
    registerTab.classList.toggle('active', registering);

    confirmGroup.classList.toggle('hidden-field', !registering);
    confirmPassword.required = registering;

    formTitle.textContent = registering
        ? 'Create your account'
        : 'Welcome back';

    formSubtitle.textContent = registering
        ? 'Create an account to start your interview preparation.'
        : 'Enter your details to continue your preparation.';

    submitButton.textContent = registering
        ? 'Create account'
        : 'Login to your account';

    switchCopy.firstChild.textContent = registering
        ? 'Already have an account? '
        : "Don't have an account? ";

    switchButton.textContent = registering
        ? 'Login'
        : 'Create one';

    showMessage('');
}

// Tab buttons
loginTab.addEventListener('click', () => {
    setMode('login');
});

registerTab.addEventListener('click', () => {
    setMode('register');
});

switchButton.addEventListener('click', () => {
    setMode(currentMode === 'login' ? 'register' : 'login');
});

// Registration and Login form submission
authForm.addEventListener('submit', async (event) => {

    event.preventDefault();

    const email = document.getElementById('email').value.trim();
    const password = passwordInput.value;

    // Registration validation
    if (currentMode === 'register') {

        if (password.length < 8) {
            showMessage(
                'Password must contain at least 8 characters.',
                'error'
            );
            return;
        }

        if (password !== confirmPassword.value) {
            showMessage(
                'Passwords do not match.',
                'error'
            );
            return;
        }
    }

    submitButton.disabled = true;

    try {

        const endpoint = currentMode === 'register'
            ? '/api/auth/register'
            : '/api/auth/login';

        const result = await sendRequest(endpoint, {
            method: 'POST',
            body: JSON.stringify({
                email: email,
                password: password
            })
        });

        // After registration, go to Login tab
        if (currentMode === 'register') {

            document.getElementById('email').value = email;

            passwordInput.value = '';
            confirmPassword.value = '';

            setMode('login');

            showMessage(
                'Registration successful! Please login now.',
                'success'
            );

            return;
        }

        // After successful login, open dashboard
        showMessage(
            'Login successful! Redirecting...',
            'success'
        );

        window.location.replace('/dashboard.html');

    } catch (error) {

        showMessage(error.message, 'error');

    } finally {

        submitButton.disabled = false;

    }
});

// Check whether user is already logged in
async function checkExistingSession() {
    try {

        await sendRequest('/api/auth/me');

        window.location.replace('/dashboard.html');

    } catch (error) {
        // No active session. Stay on login/register page.
    }
}

checkExistingSession();
