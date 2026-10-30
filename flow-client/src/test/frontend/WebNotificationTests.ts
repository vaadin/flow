import { expect } from '@open-wc/testing';

// API under test — importing registers window.Vaadin.Flow.webNotification.
import '../../main/frontend/WebNotification';

const $wnd = window as any;

// Stands in for the browser's Notification, which needs a real permission
// grant and shows operating system UI. close() fires 'close' like the real one.
class FakeNotification extends EventTarget {
  static permission: NotificationPermission = 'granted';
  static nextPermission: NotificationPermission = 'granted';
  static instances: FakeNotification[] = [];

  static requestPermission(): Promise<NotificationPermission> {
    FakeNotification.permission = FakeNotification.nextPermission;
    return Promise.resolve(FakeNotification.permission);
  }

  readonly title: string;
  readonly options: NotificationOptions;

  constructor(title: string, options: NotificationOptions) {
    super();
    this.title = title;
    this.options = options;
    FakeNotification.instances.push(this);
  }

  close(): void {
    this.dispatchEvent(new Event('close'));
  }
}

function recordingChannel(): { calls: unknown[][]; channel: (...args: unknown[]) => void } {
  const calls: unknown[][] = [];
  return { calls, channel: (...args: unknown[]) => calls.push(args) };
}

let keyCounter = 0;
function nextKey(): string {
  keyCounter += 1;
  return `key-${keyCounter}`;
}

const ignore = () => {};

// Shows a notification the way the server-side handle does, returning its key
function show(
  title: string,
  options: object,
  channel: (...args: unknown[]) => void = ignore,
  appId: string | null = 'app'
): string {
  const key = nextKey();
  $wnd.Vaadin.Flow.webNotification.show(title, options, channel, { key, appId });
  return key;
}

describe('webNotification', () => {
  let originalNotification: unknown;

  beforeEach(() => {
    originalNotification = $wnd.Notification;
    $wnd.Notification = FakeNotification;
    FakeNotification.permission = 'granted';
    FakeNotification.instances = [];
  });

  afterEach(() => {
    $wnd.Notification = originalNotification;
  });

  it('reports a click and then the close it causes', () => {
    const { calls, channel } = recordingChannel();
    show('Report ready', { body: 'March' }, channel);

    const notification = FakeNotification.instances[0];
    expect(notification.title).to.equal('Report ready');
    expect(notification.options).to.deep.equal({ body: 'March' });

    notification.dispatchEvent(new Event('click'));

    expect(calls).to.deep.equal([['click'], ['close']]);
  });

  it('reports a notification replaced by one with the same tag as closed once the new one is shown', () => {
    const first = recordingChannel();
    const second = recordingChannel();
    show('1 new message', { tag: 'inbox' }, first.channel);
    const secondKey = show('2 new messages', { tag: 'inbox' }, second.channel);
    expect(first.calls).to.deep.equal([]);

    FakeNotification.instances[1].dispatchEvent(new Event('show'));

    expect(first.calls).to.deep.equal([['close']]);
    expect(second.calls).to.deep.equal([]);

    // A late close event of the replaced notification is not reported twice.
    FakeNotification.instances[0].close();
    $wnd.Vaadin.Flow.webNotification.close(secondKey);

    expect(first.calls).to.deep.equal([['close']]);
    expect(second.calls).to.deep.equal([['close']]);
  });

  it('closes only the notifications with the given tag', () => {
    const inbox = recordingChannel();
    const other = recordingChannel();
    show('New message', { tag: 'inbox' }, inbox.channel);
    show('Build done', { tag: 'builds' }, other.channel);

    $wnd.Vaadin.Flow.webNotification.closeByTag('inbox', 'app');

    expect(inbox.calls).to.deep.equal([['close']]);
    expect(other.calls).to.deep.equal([]);
  });

  it('closes by tag only the notifications of the given application', () => {
    show('A', { tag: 'chat' }, ignore, 'appA');
    show('B', { tag: 'chat' }, ignore, 'appB');
    show('Unknown application', { tag: 'chat' }, ignore, null);
    const closed: string[] = [];
    FakeNotification.instances.forEach((n) => n.addEventListener('close', () => closed.push(n.title)));

    $wnd.Vaadin.Flow.webNotification.closeByTag('chat', 'appA');

    expect(closed).to.deep.equal(['A']);

    // An application with no known id only closes notifications with none
    $wnd.Vaadin.Flow.webNotification.closeByTag('chat', null);
    expect(closed).to.deep.equal(['A', 'Unknown application']);
  });

  [
    [
      'the constructor throws',
      () => {
        $wnd.Notification = class extends FakeNotification {
          constructor(title: string, options: NotificationOptions) {
            super(title, options);
            throw new TypeError('Illegal constructor');
          }
        };
      },
      ['error', 'UNKNOWN', 'Illegal constructor']
    ],
    [
      'the browser fires an error',
      () => {
        $wnd.Notification = class extends FakeNotification {
          constructor(title: string, options: NotificationOptions) {
            super(title, options);
            queueMicrotask(() => this.dispatchEvent(new Event('error')));
          }
        };
      },
      ['error', 'UNKNOWN', 'The browser could not show the notification']
    ]
  ].forEach(([when, failNext, expectedError]) => {
    it(`keeps the notification with the same tag when the new one cannot be shown because ${when}`, async () => {
      const first = recordingChannel();
      const firstKey = show('1 new message', { tag: 'inbox' }, first.channel);
      (failNext as () => void)();
      const second = recordingChannel();

      const secondKey = show('2 new messages', { tag: 'inbox' }, second.channel);
      await Promise.resolve();
      $wnd.Vaadin.Flow.webNotification.close(secondKey);

      expect(second.calls).to.deep.equal([expectedError]);
      expect(first.calls).to.deep.equal([]);
      $wnd.Vaadin.Flow.webNotification.close(firstKey);
      expect(first.calls).to.deep.equal([['close']]);
    });
  });

  it('keeps track of a limited number of notifications per application for closing', () => {
    // Fresh application ids keep notifications left by other tests out
    const app = `app-${nextKey()}`;
    const otherApp = `app-${nextKey()}`;
    const otherApplication = recordingChannel();
    const oldest = recordingChannel();
    show('Other application', { tag: 'other' }, otherApplication.channel, otherApp);
    show('Oldest', { tag: 'old' }, oldest.channel, app);
    const oldestNotification = FakeNotification.instances[FakeNotification.instances.length - 1];
    for (let i = 0; i < 50; i += 1) {
      show('Next', {}, ignore, app);
    }

    // The oldest is no longer kept for closing, but reports its own events
    $wnd.Vaadin.Flow.webNotification.closeByTag('old', app);
    expect(oldest.calls).to.deep.equal([]);

    // A late show event of the dropped one replaces nothing
    const newer = recordingChannel();
    show('Newer', { tag: 'old' }, newer.channel, app);
    oldestNotification.dispatchEvent(new Event('show'));
    expect(newer.calls).to.deep.equal([]);

    oldestNotification.dispatchEvent(new Event('click'));
    expect(oldest.calls).to.deep.equal([['click'], ['close']]);

    // Another application was not pushed out
    $wnd.Vaadin.Flow.webNotification.closeByTag('other', otherApp);
    expect(otherApplication.calls).to.deep.equal([['close']]);
  });

  it('does not replace anything when a notification that is already closed fires a late show event', () => {
    const first = recordingChannel();
    show('1 new message', { tag: 'inbox' }, first.channel);
    const secondKey = show('2 new messages', { tag: 'inbox' });
    $wnd.Vaadin.Flow.webNotification.close(secondKey);

    FakeNotification.instances[1].dispatchEvent(new Event('show'));

    expect(first.calls).to.deep.equal([]);
  });

  it('reports an error without showing anything when permission is not granted', () => {
    FakeNotification.permission = 'default';
    const { calls, channel } = recordingChannel();

    show('Hi', {}, channel);

    expect(FakeNotification.instances).to.be.empty;
    expect(calls).to.have.length(1);
    expect(calls[0].slice(0, 2)).to.deep.equal(['error', 'NOT_ALLOWED']);
  });

  it('publishes the answer to the permission prompt to the server', async () => {
    FakeNotification.permission = 'default';
    FakeNotification.nextPermission = 'granted';
    expect($wnd.Vaadin.Flow.webNotification.queryPermission()).to.equal('PROMPT');

    const published: string[] = [];
    const listener = (e: Event) => published.push((e as CustomEvent).detail);
    document.body.addEventListener('vaadin-web-notification-permission-change', listener);
    try {
      expect(await $wnd.Vaadin.Flow.webNotification.requestPermission()).to.equal('GRANTED');
    } finally {
      document.body.removeEventListener('vaadin-web-notification-permission-change', listener);
    }

    expect(published).to.deep.equal(['GRANTED']);
  });

  (
    [
      ['the window gets focus back', window, 'focus'],
      ['the tab is shown again', document, 'visibilitychange']
    ] as const
  ).forEach(([when, target, eventName]) => {
    it(`publishes a permission changed in the site settings when ${when}`, () => {
      FakeNotification.permission = 'default';
      $wnd.Vaadin.Flow.webNotification.queryPermission();

      const published: string[] = [];
      const listener = (e: Event) => published.push((e as CustomEvent).detail);
      document.body.addEventListener('vaadin-web-notification-permission-change', listener);
      try {
        FakeNotification.permission = 'granted';
        target.dispatchEvent(new Event(eventName));
      } finally {
        document.body.removeEventListener('vaadin-web-notification-permission-change', listener);
      }

      expect(published).to.deep.equal(['GRANTED']);
    });
  });
});
