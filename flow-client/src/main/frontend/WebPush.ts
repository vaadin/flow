/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/*
 * Client-side helpers for the web push API of the flow-webpush module, called
 * through the declared expressions of WebPush.WebPushJs.
 */

const noActiveSubscription = '{ "message": "No active subscription" }';

function urlB64ToUint8Array(base64String: string): Uint8Array<ArrayBuffer> {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/gu, '+').replace(/_/gu, '/');
  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}

async function getPushSubscription(): Promise<PushSubscription | null | undefined> {
  const registration = await navigator.serviceWorker.getRegistration();
  return registration?.pushManager?.getSubscription();
}

const $wnd = window as any;
$wnd.Vaadin ??= {};
$wnd.Vaadin.Flow ??= {};
$wnd.Vaadin.Flow.webPush = {
  async subscribe(publicKey: string): Promise<unknown> {
    const notificationPermission = await Notification.requestPermission();
    if (notificationPermission !== 'granted') {
      throw new Error('You have blocked notifications. You need to manually enable them in your browser.');
    }
    const registration = await navigator.serviceWorker.getRegistration();
    if (!registration) {
      throw new Error('Cannot get registration from service worker.');
    }
    if (!registration.pushManager) {
      throw new Error('Cannot get push manager from registration.');
    }
    const subscription = await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: urlB64ToUint8Array(publicKey)
    });
    if (!subscription) {
      throw new Error('Subscription failed. See console for exception.');
    }
    return JSON.parse(JSON.stringify(subscription));
  },

  async unsubscribe(): Promise<unknown> {
    const subscription = await getPushSubscription();
    if (subscription) {
      await subscription.unsubscribe();
      return JSON.parse(JSON.stringify(subscription));
    }
    return noActiveSubscription;
  },

  async registrationStatus(): Promise<boolean> {
    return !!(await getPushSubscription());
  },

  notificationDenied(): boolean {
    return Notification.permission === 'denied';
  },

  notificationGranted(): boolean {
    return Notification.permission === 'granted';
  },

  async getSubscription(): Promise<PushSubscription | string> {
    return (await getPushSubscription()) ?? noActiveSubscription;
  }
};

// Empty export to ensure TypeScript emits this as an ES module, which is
// required for Vite to load it via import.
export {};
