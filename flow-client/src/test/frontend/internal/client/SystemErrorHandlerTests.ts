import { expect } from '@open-wc/testing';
import { testRegistry } from './testRegistry';
import sinon from 'sinon';
import { Console } from '../../../../main/frontend/internal/client/Console';
import { SystemErrorHandler } from '../../../../main/frontend/internal/client/SystemErrorHandler';

// Beyond the Java suite: com.vaadin.client.SystemErrorHandler has no test class
// of its own.
describe('SystemErrorHandler', () => {
  function makeHandler(opts: { webComponentMode?: boolean; exported?: string[] } = {}) {
    return new SystemErrorHandler(
      testRegistry({
        ApplicationConfiguration: {
          isWebComponentMode: () => opts.webComponentMode ?? false,
          getExportedWebComponents: () => opts.exported ?? [],
          // The rest of the configuration contract; inert, as these cases only
          // drive the two above.
          getSessionExpiredError: () => null,
          getServiceUrl: () => '',
          getUIId: () => 0,
          setUIId: () => {},
          getHeartbeatInterval: () => 0
        },
        Heartbeat: { setInterval: () => {} },
        PushConfiguration: { isPushEnabled: () => false },
        MessageSender: { setPushEnabled: () => {} },
        UILifecycle: { setState: () => {} },
        MessageHandler: { handleMessage: () => {} }
      })
    );
  }

  // The notification builder is private in Java, so it is driven through the
  // public handleUnrecoverableError and inspected in the DOM.
  describe('handleUnrecoverableError', () => {
    const created: Element[] = [];
    let logged: string[];
    let errorStub: sinon.SinonStub;

    // A URL is always passed so the click/Escape handlers cannot reload the
    // page running the tests.
    function show(
      caption: string | null,
      message: string | null,
      details: string | null,
      querySelector: string | null = null
    ): Element {
      makeHandler().handleUnrecoverableError(caption, message, details, 'about:blank', querySelector);
      const host = querySelector === null ? document.body : document.querySelector(querySelector)!;
      const container = host.querySelector('.v-system-error')!;
      created.push(container);
      return container;
    }

    beforeEach(() => {
      logged = [];
      errorStub = sinon.stub(Console, 'error').callsFake((text: unknown) => {
        logged.push(String(text));
      });
    });

    afterEach(() => {
      errorStub.restore();
      created.forEach((el) => el.remove());
      created.length = 0;
    });

    it('builds a v-system-error container with a div per provided part and logs each', () => {
      const container = show('Cap', 'Msg', 'Det');

      expect(container.className).to.equal('v-system-error');
      expect(container.getAttribute('popover')).to.equal('manual');
      expect(container.parentNode).to.equal(document.body);
      expect(Array.from(container.children).map((c) => c.className)).to.deep.equal(['caption', 'message', 'details']);
      expect(Array.from(container.children).map((c) => c.textContent)).to.deep.equal(['Cap', 'Msg', 'Det']);
      expect(logged).to.deep.equal(['Cap', 'Msg', 'Det']);
    });

    it('omits parts that are null', () => {
      const container = show('only caption', null, null);
      expect(Array.from(container.children).map((c) => c.className)).to.deep.equal(['caption']);
      expect(logged).to.deep.equal(['only caption']);
    });

    it('appends to the element matched by the querySelector', () => {
      const host = document.createElement('div');
      host.id = 'sys-err-host';
      document.body.appendChild(host);
      created.push(host);

      const container = show('c', null, null, '#sys-err-host');
      expect(container.parentNode).to.equal(host);
    });

    it('shows the error container through the popover API', () => {
      const opened: Element[] = [];
      const original = HTMLElement.prototype.showPopover;
      HTMLElement.prototype.showPopover = function (this: HTMLElement): void {
        opened.push(this);
      };
      try {
        const container = show('c', null, null);
        expect(container.getAttribute('popover')).to.equal('manual');
        expect(opened).to.deep.equal([container]);
      } finally {
        HTMLElement.prototype.showPopover = original;
      }
    });

    it('appends the container to the shadow root of the matched element', () => {
      const host = document.createElement('div');
      host.id = 'sys-err-shadow-host';
      const root = host.attachShadow({ mode: 'open' });
      document.body.appendChild(host);
      created.push(host);

      makeHandler().handleUnrecoverableError('c', null, null, 'about:blank', '#sys-err-shadow-host');
      const container = root.querySelector('.v-system-error')!;
      created.push(container);

      expect(container.parentNode).to.equal(root);
    });
  });

  describe('class', () => {
    it('resynchronizes instead of redirecting when in web-component mode', () => {
      // With every part null, Java redirects outside web-component mode and
      // resynchronizes the session inside it; the resync stops the heartbeat.
      const intervals: number[] = [];
      const handler = new SystemErrorHandler(
        testRegistry({
          ApplicationConfiguration: {
            isWebComponentMode: () => true,
            getExportedWebComponents: () => [],
            getSessionExpiredError: () => null,
            getServiceUrl: () => 'about:blank#',
            getUIId: () => 0,
            setUIId: () => {},
            getHeartbeatInterval: () => 0
          },
          Heartbeat: { setInterval: (interval: number) => intervals.push(interval) },
          PushConfiguration: { isPushEnabled: () => false },
          MessageSender: { setPushEnabled: () => {} },
          UILifecycle: { setState: () => {} },
          MessageHandler: { handleMessage: () => {} }
        })
      );

      handler.handleUnrecoverableError(null, null, null, null, null);

      expect(intervals).to.deep.equal([-1]);
    });

    it('handleErrorObject extracts the error message', () => {
      const messages: string[] = [];
      const original = console.error;
      console.error = (...args: unknown[]) => messages.push(String(args[0]));
      try {
        makeHandler().handleErrorObject(new Error('boom'));
        makeHandler().handleErrorObject('plain string');
      } finally {
        console.error = original;
      }
      expect(messages).to.deep.equal(['boom', 'plain string']);
    });
  });
});
