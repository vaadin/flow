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
    $wnd.Vaadin.Flow.webNotification.show('Report ready', { body: 'March' }, nextKey(), channel);

    const notification = FakeNotification.instances[0];
    expect(notification.title).to.equal('Report ready');
    expect(notification.options).to.deep.equal({ body: 'March' });

    notification.dispatchEvent(new Event('click'));

    expect(calls).to.deep.equal([['click'], ['close']]);
  });

  it('reports a notification replaced by one with the same tag as closed', () => {
    const first = recordingChannel();
    const second = recordingChannel();
    $wnd.Vaadin.Flow.webNotification.show('1 new message', { tag: 'inbox' }, nextKey(), first.channel);
    const secondKey = nextKey();
    $wnd.Vaadin.Flow.webNotification.show('2 new messages', { tag: 'inbox' }, secondKey, second.channel);

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
    $wnd.Vaadin.Flow.webNotification.show('New message', { tag: 'inbox' }, nextKey(), inbox.channel);
    $wnd.Vaadin.Flow.webNotification.show('Build done', { tag: 'builds' }, nextKey(), other.channel);

    $wnd.Vaadin.Flow.webNotification.closeByTag('inbox');

    expect(inbox.calls).to.deep.equal([['close']]);
    expect(other.calls).to.deep.equal([]);
  });

  it('tracks notifications shown by a trigger, which passes no key and no channel, by tag', () => {
    $wnd.Vaadin.Flow.webNotification.show('Build done', { tag: 'builds' }, null, null);
    $wnd.Vaadin.Flow.webNotification.show('New message', { tag: 'inbox' }, null, null);
    const closed: string[] = [];
    FakeNotification.instances.forEach((n) => n.addEventListener('close', () => closed.push(n.title)));

    $wnd.Vaadin.Flow.webNotification.closeByTag('builds');

    expect(closed).to.deep.equal(['Build done']);
    $wnd.Vaadin.Flow.webNotification.closeByTag('inbox');
    expect(closed).to.deep.equal(['Build done', 'New message']);
  });

  it('keeps the notification with the same tag when the new one cannot be shown', () => {
    const first = recordingChannel();
    const firstKey = nextKey();
    $wnd.Vaadin.Flow.webNotification.show('1 new message', { tag: 'inbox' }, firstKey, first.channel);
    $wnd.Notification = class extends FakeNotification {
      constructor(title: string, options: NotificationOptions) {
        super(title, options);
        throw new TypeError('Illegal constructor');
      }
    };
    const second = recordingChannel();
    const secondKey = nextKey();

    $wnd.Vaadin.Flow.webNotification.show('2 new messages', { tag: 'inbox' }, secondKey, second.channel);
    $wnd.Vaadin.Flow.webNotification.close(secondKey);

    expect(second.calls).to.deep.equal([['error', 'UNKNOWN', 'Illegal constructor']]);
    expect(first.calls).to.deep.equal([]);
    $wnd.Vaadin.Flow.webNotification.close(firstKey);
    expect(first.calls).to.deep.equal([['close']]);
  });

  it('reports an error without showing anything when permission is not granted', () => {
    FakeNotification.permission = 'default';
    const { calls, channel } = recordingChannel();

    $wnd.Vaadin.Flow.webNotification.show('Hi', {}, nextKey(), channel);

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
});
