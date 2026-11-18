(function () {
    const storageKey = 'snackshot-prototype-theme';
    const savedTheme = window.localStorage.getItem(storageKey);
    const initialTheme = savedTheme === 'dark' ? 'dark' : 'light';

    function setTheme(theme) {
        const normalizedTheme = theme === 'dark' ? 'dark' : 'light';
        document.documentElement.dataset.theme = normalizedTheme;
        window.localStorage.setItem(storageKey, normalizedTheme);

        document.querySelectorAll('[data-theme-choice]').forEach(function (button) {
            button.setAttribute('aria-pressed', String(button.dataset.themeChoice === normalizedTheme));
        });
    }

    document.querySelectorAll('.controls > .toggle-btn[onclick*="toggleTheme"]').forEach(function (button) {
        const controls = button.parentElement;
        button.remove();
        if (controls.children.length === 0) controls.remove();
    });

    const deviceFrame = document.querySelector('.device-frame');
    if (deviceFrame) {
        const switcher = document.createElement('div');
        switcher.className = 'theme-switcher';
        switcher.setAttribute('role', 'group');
        switcher.setAttribute('aria-label', 'Color theme');
        switcher.innerHTML = '<button type="button" data-theme-choice="light">Light</button><button type="button" data-theme-choice="dark">Dark</button>';
        const firstControls = document.querySelector('body > .controls');
        document.body.insertBefore(switcher, firstControls || deviceFrame);
    }

    document.addEventListener('click', function (event) {
        const button = event.target.closest('[data-theme-choice]');
        if (button) setTheme(button.dataset.themeChoice);
    });

    window.setPrototypeTheme = setTheme;
    setTheme(initialTheme);
})();