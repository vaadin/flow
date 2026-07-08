import { expect } from '@open-wc/testing';
import {
  getShadowRootElement,
  handleError,
  recreateNodes,
  showPopover,
  SystemErrorHandler
} from '../../main/frontend/internal/SystemErrorHandler';

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
  });

  it('recreateNodes replaces matching elements with a shallow clone', () => {
    const el = document.createElement('x-recreate-test');
    (el as unknown as { $server: Record<string, unknown> }).$server = {};
    (el as unknown as { marker: boolean }).marker = true; // expando, not cloned
    document.body.appendChild(el);

    recreateNodes('x-recreate-test');

    const after = document.body.querySelector('x-recreate-test');
    expect(after).to.not.equal(null);
    expect(after).to.not.equal(el); // it was replaced
    expect((after as unknown as { marker?: boolean }).marker).to.equal(undefined);
    // the mock disconnected callback was installed on the original
    expect((el as unknown as { $server: { disconnected: unknown } }).$server.disconnected).to.be.a('function');

    after?.remove();
  });

  it('showPopover calls the element popover API when present', () => {
    let opened = false;
    const el = {
      showPopover: () => {
        opened = true;
      }
    } as unknown as Element;
    showPopover(el);
    expect(opened).to.be.true;
  });

  it('showPopover is a no-op when the element has no showPopover', () => {
    // Note: a real element may expose showPopover and throw on non-popovers, so
    // use one without the method to exercise the guard.
    const el = {} as unknown as Element;
    expect(() => showPopover(el)).to.not.throw();
  });

  it('getShadowRootElement returns the shadow root', () => {
    const host = document.createElement('div');
    const root = host.attachShadow({ mode: 'open' });
    expect(getShadowRootElement(host)).to.equal(root);
    expect(getShadowRootElement(document.createElement('div'))).to.equal(null);
  });

  describe('class', () => {
    function makeHandler(opts: { webComponentMode?: boolean; exported?: string[] } = {}) {
      return new SystemErrorHandler({
        getApplicationConfiguration: () => ({
          isWebComponentMode: () => opts.webComponentMode ?? false,
          getExportedWebComponents: () => opts.exported ?? []
        })
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
