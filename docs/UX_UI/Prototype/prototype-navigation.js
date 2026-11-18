(function () {
    const routes = {
        welcome: 's01_welcome.html',
        privacy: 's01a_privacy.html',
        home: 's02_home.html',
        homeSavedLocal: 's02_home.html?state=saved-local',
        camera: 's03_camera.html',
        gallery: 's04_gallery.html',
        recognition: 's05_progress.html',
        edit: 's07_edit_nutrition.html',
        search: 's08_manual_food_search.html',
        quickAdd: 's09_quick_add.html',
        savePrompt: 's10_health_connect_prompt.html',
        healthConnect: 's11_health_connect_authorization.html',
        dailyDashboard: 's12_daily_dashboard.html',
        weeklyDashboard: 's12_weekly_dashboard.html',
        monthDashboard: 's12_range_dashboard.html?range=month',
        threeMonthDashboard: 's12_range_dashboard.html?range=threeMonth',
        yearDashboard: 's12_range_dashboard.html?range=year',
        history: 's14_history_feed.html',
        mealReview: 's15_meal_review.html',
        mealReviewSaved: 's15_meal_review.html?mode=saved',
        settings: 's16_settings.html',
        paywall: 's19_paywall.html',
        waitlist: 's19_paywall.html?state=waitlist',
        googleHealthConnection: 's16_settings.html?view=google-health'
    };

    function navigate(route) {
        const destination = routes[route];
        if (destination) window.location.href = destination;
    }

    document.addEventListener('click', function (event) {
        const control = event.target.closest('[data-route], [data-confirm-route]');
        if (!control) return;

        if (control.dataset.confirmRoute) {
            if (!window.confirm('Delete this meal from your history?')) return;
            navigate(control.dataset.confirmRoute);
            return;
        }

        navigate(control.dataset.route);
    });

    if (document.body.dataset.screen === 'recognition') {
        window.setTimeout(function () {
            navigate('mealReview');
        }, 5000);
    }
})();