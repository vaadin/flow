import { expect } from '@open-wc/testing';
import sinon from 'sinon';
import { ACTIVITY_CHANGED_EVENT } from '../../main/frontend/internal/client/ActivityNotifier';

const $wnd = window as any;

let readyScript: string;

function installReady(flow: object) {
  $wnd.Vaadin = { Flow: flow };
  new Function(readyScript)();
}

function client(active: boolean) {
  return { isActive: () => active };
}

describe('window.Vaadin.Flow.ready', () => {
  let savedVaadin: unknown;

  before(async () => {
    // Served by web-test-runner.config.mjs from flow-server, where the script
    // is inlined into the bootstrap page. The server joins its lines without a
    // separator when inlining it, so evaluate it in that form.
    readyScript = (await (await fetch('/flow-server/ready.js')).text()).split(/\r?\n/).join('');
  });

  beforeEach(() => {
    savedVaadin = $wnd.Vaadin;
  });

  afterEach(() => {
    $wnd.Vaadin = savedVaadin;
  });

  it('resolves immediately when all clients are idle', async () => {
    installReady({ clients: { a: client(false), b: client(false) } });
    await $wnd.Vaadin.Flow.ready({ timeout: 100 });
  });

  it('resolves when there are no Flow clients', async () => {
    installReady({});
    await $wnd.Vaadin.Flow.ready({ timeout: 100 });
  });

  it('resolves when a client reports that it became idle', async () => {
    let active = true;
    installReady({ clients: { a: { isActive: () => active } } });
    let resolved = false;
    const ready = $wnd.Vaadin.Flow.ready({ timeout: 5000 }).then(() => {
      resolved = true;
    });

    await new Promise((r) => setTimeout(r, 20));
    expect(resolved).to.be.false;

    active = false;
    const start = performance.now();
    window.dispatchEvent(new Event(ACTIVITY_CHANGED_EVENT));
    await ready;
    // Well below the safety net interval, i.e. resolved by the event
    expect(performance.now() - start).to.be.below(100);
  });

  it('waits for the document to finish loading', async () => {
    installReady({ clients: {} });
    let readyState = 'loading';
    const readyStateStub = sinon.stub(document, 'readyState').get(() => readyState);
    try {
      let resolved = false;
      const ready = $wnd.Vaadin.Flow.ready({ timeout: 5000 }).then(() => {
        resolved = true;
      });

      await new Promise((r) => setTimeout(r, 20));
      expect(resolved).to.be.false;

      readyState = 'complete';
      document.dispatchEvent(new Event('readystatechange'));
      await ready;
    } finally {
      readyStateStub.restore();
    }
  });

  it('keeps waiting while the dev server is not loaded', async () => {
    installReady({ devServerIsNotLoaded: true, clients: {} });
    let error: Error | undefined;
    try {
      await $wnd.Vaadin.Flow.ready({ timeout: 50 });
    } catch (e) {
      error = e as Error;
    }
    expect(error?.message).to.equal('Vaadin.Flow.ready timed out after 50ms');
  });

  it('rejects when a client stays active until the timeout', async () => {
    installReady({ clients: { a: client(true) } });
    let error: Error | undefined;
    try {
      await $wnd.Vaadin.Flow.ready({ timeout: 50 });
    } catch (e) {
      error = e as Error;
    }
    expect(error?.message).to.equal('Vaadin.Flow.ready timed out after 50ms');
  });
});
