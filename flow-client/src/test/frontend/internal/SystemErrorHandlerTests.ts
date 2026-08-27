import { expect } from '@open-wc/testing';
import { handleError, SystemErrorHandler } from '../../../main/frontend/internal/client/SystemErrorHandler';

// Beyond the Java suite: com.vaadin.client.SystemErrorHandler has no test class
// of its own.
describe('SystemErrorHandler', () => {
  describe('handleError', () => {
    const created: Element[] = [];
    const noop = (): void => {};
    afterEach(() => {
      created.forEach((el) => el.remove());
      created.length = 0;
    });

    it('builds a v-system-error container with a div per provided part and logs each', () => {
      const logged: string[] = [];
      const container = handleError('Cap', 'Msg', 'Det', null, (t) => logged.push(t));
      created.push(container);

      expect(container.className).to.equal('v-system-error');
      expect(container.getAttribute('popover')).to.equal('manual');
      expect(container.parentNode).to.equal(document.body);
      expect(Array.from(container.children).map((c) => c.className)).to.deep.equal(['caption', 'message', 'details']);
      expect(Array.from(container.children).map((c) => c.textContent)).to.deep.equal(['Cap', 'Msg', 'Det']);
      expect(logged).to.deep.equal(['Cap', 'Msg', 'Det']);
    });

    it('omits parts that are null', () => {
      const logged: string[] = [];
      const container = handleError('only caption', null, null, null, (t) => logged.push(t));
      created.push(container);
      expect(Array.from(container.children).map((c) => c.className)).to.deep.equal(['caption']);
      expect(logged).to.deep.equal(['only caption']);
    });

    it('appends to the element matched by the querySelector', () => {
      const host = document.createElement('div');
      host.id = 'sys-err-host';
      document.body.appendChild(host);
      created.push(host);

      const container = handleError('c', null, null, '#sys-err-host', noop);
      expect(container.parentNode).to.equal(host);
    });

    it('shows the error container through the popover API', () => {
      // handleError sets popover="manual" on the container and shows it; the
      // native call is observed through the element the function returns.
      const opened: Element[] = [];
      const original = HTMLElement.prototype.showPopover;
      HTMLElement.prototype.showPopover = function (this: HTMLElement): void {
        opened.push(this);
      };
      try {
        const container = handleError('c', null, null, null, noop);
        created.push(container);
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

      const container = handleError('c', null, null, '#sys-err-shadow-host', noop);

      expect(container.parentNode).to.equal(root);
    });
  });

  describe('class', () => {
    function makeHandler(opts: { webComponentMode?: boolean; exported?: string[] } = {}) {
      return new SystemErrorHandler({
        getApplicationConfiguration: () => ({
          isWebComponentMode: () => opts.webComponentMode ?? false,
          getExportedWebComponents: () => opts.exported ?? [],
          // The rest of the configuration contract; inert, as these cases only
          // drive the two above.
          getSessionExpiredError: () => null,
          getServiceUrl: () => '',
          getUIId: () => 0,
          setUIId: () => {},
          getHeartbeatInterval: () => 0
        }),
        getHeartbeat: () => ({ setInterval: () => {} }),
        getPushConfiguration: () => ({ isPushEnabled: () => false }),
        getMessageSender: () => ({ setPushEnabled: () => {} }),
        getUILifecycle: () => ({ setState: () => {} }),
        getMessageHandler: () => ({ handleMessage: () => {} }),
        reset: () => {}
      });
    }

    it('reflects web-component mode from the configuration', () => {
      expect(makeHandler({ webComponentMode: true }).isWebComponentMode()).to.be.true;
      expect(makeHandler({ webComponentMode: false }).isWebComponentMode()).to.be.false;
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

    it('recreateWebComponents recreates each exported component (clones stale elements)', () => {
      // Register a stale custom element instance with a $server stub.
      const stale = document.createElement('x-stale-probe') as unknown as Element & {
        $server: { disconnected: () => void };
      };
      stale.$server = { disconnected: () => {} };
      document.body.appendChild(stale);
      try {
        makeHandler({ exported: ['x-stale-probe'] }).recreateWebComponents();
        // The original stale element was replaced by a clone (different identity).
        const after = document.getElementsByTagName('x-stale-probe')[0];
        expect(after).to.not.equal(stale);
      } finally {
        document.getElementsByTagName('x-stale-probe')[0]?.remove();
      }
    });
  });
});
