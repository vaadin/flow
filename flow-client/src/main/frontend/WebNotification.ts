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

type VaadinWebNotificationPermission = 'PROMPT' | 'GRANTED' | 'DENIED' | 'UNSUPPORTED';
type VaadinWebNotificationErrorReason = 'UNSUPPORTED' | 'NOT_ALLOWED' | 'UNKNOWN';

/**
 * Options for a notification, matching the Java WebNotificationOptions record.
 */
interface VaadinWebNotificationOptions {
  body?: string;
  icon?: string;
  tag?: string;
  renotify?: boolean;
  requireInteraction?: boolean;
  silent?: boolean;
}

/**
 * Return channel of the server-side handle. Called with 'click', 'close', or
 * 'error' followed by the reason and a message.
 */
type VaadinWebNotificationChannel = (
  type: 'click' | 'close' | 'error',
  reason?: VaadinWebNotificationErrorReason,
  message?: string
) => void;

interface ShownNotification {
  notification: Notification;
  tag?: string;
  channel: VaadinWebNotificationChannel;
}

// Notifications shown by this page, keyed by the id the server generated for
// the handle, so that close(key) does not need a browser-side id to travel back.
const shown = new Map<string, ShownNotification>();

let cachedPermission: VaadinWebNotificationPermission | null = null;
let permissionListenersInstalled = false;

function isSupported(): boolean {
  return window.isSecureContext && 'Notification' in window;
}

function currentPermission(): VaadinWebNotificationPermission {
  if (!isSupported()) {
    return 'UNSUPPORTED';
  }
  switch (Notification.permission) {
    case 'granted':
      return 'GRANTED';
    case 'denied':
      return 'DENIED';
    default:
      return 'PROMPT';
  }
}

function publishPermission(): VaadinWebNotificationPermission {
  const next = currentPermission();
  if (cachedPermission !== next) {
    cachedPermission = next;
    // Dispatch on document.body, the UI element on the server, where UIInternals
    // listens for it.
    document.body.dispatchEvent(new CustomEvent('vaadin-web-notification-permission-change', { detail: next }));
  }
  return next;
}

function installPermissionListeners(): void {
  if (permissionListenersInstalled || !isSupported()) {
    return;
  }
  permissionListenersInstalled = true;
  navigator.permissions.query({ name: 'notifications' }).then(
    (status) => status.addEventListener('change', () => publishPermission()),
    () => {
      // The permission name is not queryable here; the visibility check below
      // still picks up changes.
    }
  );
  // A change made in the browser's site settings does not fire a permission
  // change event in every supported browser, and the user has to leave the tab
  // to make it, so check again whenever the tab is shown.
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
      publishPermission();
    }
  });
}

function forget(key: string, entry: ShownNotification): boolean {
  if (shown.get(key) !== entry) {
    return false;
  }
  shown.delete(key);
  return true;
}

const $wnd = window as any;
$wnd.Vaadin ??= {};
$wnd.Vaadin.Flow ??= {};
$wnd.Vaadin.Flow.webNotification = {
  queryPermission(): VaadinWebNotificationPermission {
    installPermissionListeners();
    cachedPermission = currentPermission();
    return cachedPermission;
  },

  // Must be called synchronously from the click handler: the browser only
  // shows the permission prompt during a user gesture. The async function body
  // runs synchronously up to the first await, so requestPermission() still
  // sees the gesture.
  async requestPermission(): Promise<VaadinWebNotificationPermission> {
    if (!isSupported()) {
      return 'UNSUPPORTED';
    }
    await Notification.requestPermission();
    return publishPermission();
  },

  show(title: string, options: VaadinWebNotificationOptions, key: string, channel: VaadinWebNotificationChannel): void {
    if (!isSupported()) {
      channel(
        'error',
        'UNSUPPORTED',
        window.isSecureContext
          ? 'Notifications API not implemented in this browser'
          : 'Notifications API requires a secure context (HTTPS or localhost)'
      );
      return;
    }
    if (Notification.permission !== 'granted') {
      channel('error', 'NOT_ALLOWED', 'Notification permission has not been granted');
      return;
    }
    let notification: Notification;
    try {
      notification = new Notification(title, options);
    } catch (e: any) {
      // Chrome on Android only shows notifications through a service worker
      // and throws a TypeError from the constructor.
      channel('error', 'UNKNOWN', e?.message ? String(e.message) : String(e));
      return;
    }
    if (options.tag) {
      // The browser replaced a notification that has the same tag, so the
      // previous one is gone for the user even though it fires no close event.
      shown.forEach((previous, previousKey) => {
        if (previous.tag === options.tag) {
          shown.delete(previousKey);
          previous.channel('close');
        }
      });
    }
    const entry: ShownNotification = { notification, tag: options.tag, channel };
    shown.set(key, entry);
    notification.addEventListener('click', () => {
      window.focus();
      channel('click');
      notification.close();
    });
    notification.addEventListener('close', () => {
      if (forget(key, entry)) {
        channel('close');
      }
    });
    notification.addEventListener('error', () => {
      if (forget(key, entry)) {
        channel('error', 'UNKNOWN', 'The browser could not show the notification');
      }
    });
  },

  close(key: string): void {
    shown.get(key)?.notification.close();
  },

  closeByTag(tag: string): void {
    shown.forEach((entry) => {
      if (entry.tag === tag) {
        entry.notification.close();
      }
    });
  }
};

// Empty export to ensure TypeScript emits this as an ES module, which is
// required for Vite to load it via import.
export {};
