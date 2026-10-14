/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};

window.Vaadin.Flow.webPush = window.Vaadin.Flow.webPush || {
    subscribe: async function (publicKey) {
        const notificationPermission = await Notification.requestPermission();
        if (notificationPermission === 'granted') {
            const registration = await navigator.serviceWorker.getRegistration();

            if (registration) {
                if (registration?.pushManager) {
                    const subscription = await registration?.pushManager.subscribe({
                        userVisibleOnly: true,
                        applicationServerKey: this.urlB64ToUint8Array(publicKey),
                    });

                    if (subscription) {
                        return JSON.parse(JSON.stringify(subscription));
                    }
                    throw new Error("Subscription failed. See console for exception.");
                }
                throw new Error("Cannot get push manager from registration.");
            }
            throw new Error("Cannot get registration from service worker.");
        }
        throw new Error("You have blocked notifications. You need to manually enable them in your browser.");
    },

    unsubscribe: async function () {
        const registration = await navigator.serviceWorker.getRegistration();
        const subscription = await registration?.pushManager.getSubscription();
        if (subscription) {
            await subscription.unsubscribe();

            return JSON.parse(JSON.stringify(subscription));
        }
        return '{ "message": "No active subscription" }';
    },

    registrationStatus: async function () {
        const registration = await navigator.serviceWorker.getRegistration();
        return !!(await registration?.pushManager.getSubscription());
    },

    notificationDenied: async function () {
        return Notification.permission === 'denied';
    },

    notificationGranted: async function () {
        return Notification.permission === 'granted';
    },

    getSubscription: async function () {
        const registration = await navigator.serviceWorker.getRegistration();
        const subscription = await registration?.pushManager.getSubscription();
        if (subscription) {
            return subscription;
        }
        return '{ "message": "No active subscription" }';
    },

    urlB64ToUint8Array(base64String) {
        const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
        const base64 = (base64String + padding).replace(/\-/g, '+').replace(/_/g, '/');
        const rawData = window.atob(base64);
        const outputArray = new Uint8Array(rawData.length);
        for (let i = 0; i < rawData.length; ++i) {
            outputArray[i] = rawData.charCodeAt(i);
        }
        return outputArray;
    }
}
