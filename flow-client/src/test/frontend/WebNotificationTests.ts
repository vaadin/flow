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
    $wnd.Vaadin.Flow.webNotification.show('Report ready', { body: 'March' }, channel, { key: nextKey(), owner: 'app' });

    const notification = FakeNotification.instances[0];
    expect(notification.title).to.equal('Report ready');
    expect(notification.options).to.deep.equal({ body: 'March' });

    notification.dispatchEvent(new Event('click'));

    expect(calls).to.deep.equal([['click'], ['close']]);
  });

  it('reports a notification replaced by one with the same tag as closed once the new one is shown', () => {
    const first = recordingChannel();
    const second = recordingChannel();
    $wnd.Vaadin.Flow.webNotification.show('1 new message', { tag: 'inbox' }, first.channel, {
      key: nextKey(),
      owner: 'app'
    });
    const secondKey = nextKey();
    $wnd.Vaadin.Flow.webNotification.show('2 new messages', { tag: 'inbox' }, second.channel, {
      key: secondKey,
      owner: 'app'
    });
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
    $wnd.Vaadin.Flow.webNotification.show('New message', { tag: 'inbox' }, inbox.channel, {
      key: nextKey(),
      owner: 'app'
    });
    $wnd.Vaadin.Flow.webNotification.show('Build done', { tag: 'builds' }, other.channel, {
      key: nextKey(),
      owner: 'app'
    });

    $wnd.Vaadin.Flow.webNotification.closeByTag('inbox', 'app');

    expect(inbox.calls).to.deep.equal([['close']]);
    expect(other.calls).to.deep.equal([]);
  });

  it('closes by tag only the notifications of the given application, also those shown by a trigger', () => {
    const originalClients = $wnd.Vaadin.Flow.clients;
    const hostA = document.createElement('button');
    const hostB = document.createElement('button');
    const lateHostA = document.createElement('button');
    let lateHostRegistered = false;
    $wnd.Vaadin.Flow.clients = {
      appA: {
        getNodeId: (element: Element) => (element === hostA || (element === lateHostA && lateHostRegistered) ? 1 : -1)
      },
      appB: { getNodeId: (element: Element) => (element === hostB ? 1 : -1) }
    };
    try {
      // A trigger passes no key and no channel, and its host as the owner
      $wnd.Vaadin.Flow.webNotification.show('A from trigger', { tag: 'chat' }, null, { owner: hostA });
      $wnd.Vaadin.Flow.webNotification.show('B from trigger', { tag: 'chat' }, null, { owner: hostB });
      $wnd.Vaadin.Flow.webNotification.show('A from server', { tag: 'chat' }, null, { key: nextKey(), owner: 'appA' });
      $wnd.Vaadin.Flow.webNotification.show('B from server', { tag: 'chat' }, null, { key: nextKey(), owner: 'appB' });
      // A host that fires before its application has registered it
      $wnd.Vaadin.Flow.webNotification.show('A from late host', { tag: 'chat' }, null, { owner: lateHostA });
      $wnd.Vaadin.Flow.webNotification.show('Unknown owner', { tag: 'chat' }, null, {
        owner: document.createElement('div')
      });
      lateHostRegistered = true;
      const closed: string[] = [];
      FakeNotification.instances.forEach((n) => n.addEventListener('close', () => closed.push(n.title)));

      $wnd.Vaadin.Flow.webNotification.closeByTag('chat', 'appA');

      expect(closed).to.deep.equal(['A from trigger', 'A from server', 'A from late host']);
    } finally {
      $wnd.Vaadin.Flow.clients = originalClients;
    }
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
      const firstKey = nextKey();
      $wnd.Vaadin.Flow.webNotification.show('1 new message', { tag: 'inbox' }, first.channel, {
        key: firstKey,
        owner: 'app'
      });
      (failNext as () => void)();
      const second = recordingChannel();
      const secondKey = nextKey();

      $wnd.Vaadin.Flow.webNotification.show('2 new messages', { tag: 'inbox' }, second.channel, {
        key: secondKey,
        owner: 'app'
      });
      await Promise.resolve();
      $wnd.Vaadin.Flow.webNotification.close(secondKey);

      expect(second.calls).to.deep.equal([expectedError]);
      expect(first.calls).to.deep.equal([]);
      $wnd.Vaadin.Flow.webNotification.close(firstKey);
      expect(first.calls).to.deep.equal([['close']]);
    });
  });

  it('keeps track of a limited number of notifications for closing, while still reporting their events', () => {
    const first = recordingChannel();
    const firstKey = nextKey();
    $wnd.Vaadin.Flow.webNotification.show('First', { tag: 'old' }, first.channel, { key: firstKey, owner: 'app' });
    for (let i = 0; i < 50; i += 1) {
      $wnd.Vaadin.Flow.webNotification.show('Next', {}, null, { key: nextKey(), owner: 'app' });
    }

    $wnd.Vaadin.Flow.webNotification.close(firstKey);
    $wnd.Vaadin.Flow.webNotification.closeByTag('old', 'app');
    expect(first.calls).to.deep.equal([]);

    FakeNotification.instances[0].dispatchEvent(new Event('click'));
    expect(first.calls).to.deep.equal([['click'], ['close']]);
  });

  it('reports an error without showing anything when permission is not granted', () => {
    FakeNotification.permission = 'default';
    const { calls, channel } = recordingChannel();

    $wnd.Vaadin.Flow.webNotification.show('Hi', {}, channel, { key: nextKey(), owner: 'app' });

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
