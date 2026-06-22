import { expect } from '@open-wc/testing';
import { getShadowRootElement, recreateNodes, showPopover } from '../../main/frontend/internal/SystemErrorHandler';

describe('SystemErrorHandler', () => {
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
});
