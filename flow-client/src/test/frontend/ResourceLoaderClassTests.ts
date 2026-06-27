import { expect } from '@open-wc/testing';
import { ResourceLoader } from '../../main/frontend/internal/ResourceLoader';

const settle = (ms = 50) => new Promise((resolve) => setTimeout(resolve, ms));

function recordingListener() {
  const calls: string[] = [];
  return { calls, listener: { onLoad: () => calls.push('load'), onError: () => calls.push('error') } };
}

describe('ResourceLoader (class)', () => {
  it('loads a dynamic import and reports success', async () => {
    const loader = new ResourceLoader({ handleError: () => {} }, false);
    const { calls, listener } = recordingListener();
    loader.loadDynamicImport('return Promise.resolve();', listener);
    await settle();
    expect(calls).to.deep.equal(['load']);
  });

  it('reports a rejected dynamic import as an error', async () => {
    const loader = new ResourceLoader({ handleError: () => {} }, false);
    const { calls, listener } = recordingListener();
    loader.loadDynamicImport('return Promise.reject(new Error("nope"));', listener);
    await settle();
    expect(calls).to.deep.equal(['error']);
  });

  it('loads an external script and dedupes a repeat request', async () => {
    const loader = new ResourceLoader({ handleError: () => {} }, false);
    const url = 'data:text/javascript,globalThis.__rl_probe=(globalThis.__rl_probe||0)+1';
    const first = recordingListener();
    loader.loadScript(url, first.listener);
    await settle();
    expect(first.calls).to.deep.equal(['load']);

    // Already loaded: a second request notifies immediately, no re-load.
    const second = recordingListener();
    loader.loadScript(url, second.listener);
    expect(second.calls).to.deep.equal(['load']);
  });

  it('runs a task immediately when HTML imports are unsupported', () => {
    const loader = new ResourceLoader({ handleError: () => {} }, false);
    let ran = false;
    // jsdom/Chromium test env has no HTMLImports.whenReady -> runs immediately.
    loader.runWhenHtmlImportsReady(() => {
      ran = true;
    });
    expect(ran).to.be.true;
  });
});
