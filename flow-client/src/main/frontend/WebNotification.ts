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

/**
 * Where a notification comes from: the key the server-side handle closes it by,
 * and the application showing it, either its id or, from a trigger, the
 * trigger's host element.
 */
interface VaadinWebNotificationSource {
  key?: string | null;
  owner?: string | Element | null;
}

interface ShownNotification {
  notification: Notification;
  tag?: string;
  // The application that showed the notification, null when not known
  appId: string | null;
  channel: VaadinWebNotificationChannel;
}

// Notifications shown by this page, keyed by the id the server generated for
// the handle, so that close(key) does not need a browser-side id to travel back.
const shown = new Map<string, ShownNotification>();

const $wnd = window as any;

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
      // The permission name is not queryable here; the visibility and focus
      // checks below still pick up changes.
    }
  );
  // A change made in the browser's site settings is not reported by a
  // permission change event in some browsers (e.g. Safari), so check again
  // when the user returns: the tab is shown again after the settings were changed in
  // another tab, or the window gets focus back from the site settings popup
  // next to the address bar, which does not hide the tab.
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
      publishPermission();
    }
  });
  window.addEventListener('focus', () => publishPermission());
}

/**
 * Finds the id of the Flow application that renders the given element, so that
 * notifications are scoped to it when several applications share the page.
 */
function appIdOf(element: Element): string | null {
  const clients: Record<string, any> = ($wnd.Vaadin?.Flow?.clients as Record<string, any>) ?? {};
  return Object.keys(clients).find((appId) => (clients[appId]?.getNodeId?.(element) ?? -1) >= 0) ?? null;
}

function belongsTo(entry: ShownNotification, appId: string | null | undefined): boolean {
  return !appId || entry.appId === null || entry.appId === appId;
}

function forget(key: string, entry: ShownNotification): boolean {
  if (shown.get(key) !== entry) {
    return false;
  }
  shown.delete(key);
  return true;
}

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

  show(
    title: string,
    options: VaadinWebNotificationOptions,
    channel: VaadinWebNotificationChannel | null,
    source: VaadinWebNotificationSource
  ): void {
    // A trigger-bound action passes no key, since it closes notifications by
    // tag only, and no channel when the server is not interested in events.
    const report: VaadinWebNotificationChannel = channel ?? (() => {});
    if (!isSupported()) {
      report(
        'error',
        'UNSUPPORTED',
        window.isSecureContext
          ? 'Notifications API not implemented in this browser'
          : 'Notifications API requires a secure context (HTTPS or localhost)'
      );
      return;
    }
    if (Notification.permission !== 'granted') {
      report('error', 'NOT_ALLOWED', 'Notification permission has not been granted');
      return;
    }
    let notification: Notification;
    try {
      notification = new Notification(title, options);
    } catch (e: any) {
      // Chrome on Android only shows notifications through a service worker
      // and throws a TypeError from the constructor.
      report('error', 'UNKNOWN', e?.message ? String(e.message) : String(e));
      return;
    }
    const entryKey = source.key ?? crypto.randomUUID();
    const owner = source.owner ?? null;
    const appId = owner instanceof Element ? appIdOf(owner) : owner;
    const entry: ShownNotification = { notification, tag: options.tag, appId, channel: report };
    shown.set(entryKey, entry);
    notification.addEventListener('show', () => {
      if (!options.tag) {
        return;
      }
      // The browser replaced the notifications shown earlier with the same
      // tag, so they are gone for the user even though they fire no close
      // event. Only known once this one is shown: if it fails, the earlier
      // ones stay on screen.
      for (const [previousKey, previous] of shown) {
        if (previous === entry) {
          break;
        }
        if (previous.tag === options.tag) {
          shown.delete(previousKey);
          previous.channel('close');
        }
      }
    });
    notification.addEventListener('click', () => {
      window.focus();
      if (shown.get(entryKey) === entry) {
        report('click');
      }
      notification.close();
    });
    notification.addEventListener('close', () => {
      if (forget(entryKey, entry)) {
        report('close');
      }
    });
    notification.addEventListener('error', () => {
      if (forget(entryKey, entry)) {
        report('error', 'UNKNOWN', 'The browser could not show the notification');
      }
    });
  },

  close(key: string): void {
    shown.get(key)?.notification.close();
  },

  // The server no longer listens to the notification; it stays visible but
  // reports nothing more.
  forget(key: string): void {
    shown.delete(key);
  },

  closeByTag(tag: string, appId: string | null | undefined): void {
    shown.forEach((entry) => {
      if (entry.tag === tag && belongsTo(entry, appId)) {
        entry.notification.close();
      }
    });
  }
};

// Empty export to ensure TypeScript emits this as an ES module, which is
// required for Vite to load it via import.
export {};
