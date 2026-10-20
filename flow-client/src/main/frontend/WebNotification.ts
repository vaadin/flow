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
  key: string;
  notification: Notification;
  tag?: string;
  // The application that showed the notification, null when not known
  appId: string | null;
  // The element of the trigger that showed the notification, if any
  host: Element | null;
  // Whether a server-side handle showed the notification, rather than a trigger
  fromServer: boolean;
  channel: VaadinWebNotificationChannel;
  // Whether the close or error has been reported, after which nothing more is
  // reported
  done: boolean;
}

// How many notifications the page keeps track of for closing them, counted
// separately per application for the server-side handles and for triggers. A
// notification the browser never reports as closed, for example because the
// operating system moved it to its notification center, would otherwise stay
// here for the life of the page. Matches the limit of the server, which closes
// its oldest handle before showing one more, so a handle is never dropped here
// while the server still tracks it.
const MAX_SHOWN = 50;

// Notifications shown by this page, in the order they were shown, keyed by the
// id the server generated for the handle, so that close(key) does not need a
// browser-side id to travel back.
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
  // A trigger's host is resolved when shown, since a detached host no longer
  // belongs to any application, and again when closing, since a host may be
  // registered with its application only after the trigger fired.
  const entryAppId = entry.appId ?? (entry.host ? appIdOf(entry.host) : null);
  return entryAppId === (appId ?? null);
}

function finish(entry: ShownNotification): boolean {
  if (entry.done) {
    return false;
  }
  entry.done = true;
  if (shown.get(entry.key) === entry) {
    shown.delete(entry.key);
  }
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
    const entry: ShownNotification = {
      key: source.key ?? crypto.randomUUID(),
      notification,
      tag: options.tag,
      appId: source.owner instanceof Element ? appIdOf(source.owner) : (source.owner ?? null),
      host: source.owner instanceof Element ? source.owner : null,
      fromServer: source.key != null,
      channel: report,
      done: false
    };
    shown.set(entry.key, entry);
    const group = [...shown.values()].filter(
      (other) => other.appId === entry.appId && other.fromServer === entry.fromServer
    );
    if (group.length > MAX_SHOWN) {
      // Only stops tracking the oldest one for closing; its own events are
      // still reported.
      shown.delete(group[0].key);
    }
    notification.addEventListener('show', () => {
      // Nothing to replace for a notification without a tag, or one that is
      // already gone or no longer tracked, which would otherwise be taken for
      // the newest one.
      if (!options.tag || entry.done || shown.get(entry.key) !== entry) {
        return;
      }
      // The browser replaced the notifications shown earlier with the same
      // tag, so they are gone for the user even though they fire no close
      // event. Only known once this one is shown: if it fails, the earlier
      // ones stay on screen.
      for (const previous of [...shown.values()]) {
        if (previous === entry) {
          break;
        }
        if (previous.tag === options.tag && finish(previous)) {
          previous.channel('close');
        }
      }
    });
    notification.addEventListener('click', () => {
      window.focus();
      if (!entry.done) {
        report('click');
      }
      notification.close();
    });
    notification.addEventListener('close', () => {
      if (finish(entry)) {
        report('close');
      }
    });
    notification.addEventListener('error', () => {
      if (finish(entry)) {
        report('error', 'UNKNOWN', 'The browser could not show the notification');
      }
    });
  },

  close(key: string): void {
    shown.get(key)?.notification.close();
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
