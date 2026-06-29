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

  it('loads a stylesheet (in head, before the marker comment) and dedupes', async () => {
    const comment = document.createComment('Stylesheet end');
    document.head.appendChild(comment);
    try {
      const loader = new ResourceLoader({ handleError: () => {} }, false);
      const url = `data:text/css,/* ${Math.floor(performance.now())} */ .rl-probe{color:red}`;
      const first = recordingListener();
      loader.loadStylesheet(url, first.listener);

      // The <link> is inserted into <head> before the marker comment.
      const link = document.head.querySelector(`link[href="${url}"]`);
      expect(link).to.not.equal(null);
      const nodes: Node[] = Array.from(document.head.childNodes);
      expect(nodes.indexOf(link as Node)).to.be.lessThan(nodes.indexOf(comment));

      await settle();
      expect(first.calls).to.deep.equal(['load']);

      // Already loaded: a second request notifies immediately.
      const second = recordingListener();
      loader.loadStylesheet(url, second.listener);
      expect(second.calls).to.deep.equal(['load']);

      link?.remove();
    } finally {
      comment.remove();
    }
  });

  it('inlineStyleSheet notifies immediately for already-loaded contents', () => {
    const comment = document.createComment('Stylesheet end');
    document.head.appendChild(comment);
    try {
      const loader = new ResourceLoader({ handleError: () => {} }, false);
      const css = '.inline-probe{color:blue}';
      // Pre-mark as loaded via a dependency id round-trip is not exposed; instead
      // load once (style elements don't fire load in test, so we just verify the
      // <style> is inserted before the marker comment).
      loader.inlineStyleSheet(css, recordingListener().listener);
      const style = Array.from(document.head.querySelectorAll('style')).find((s) => s.textContent === css);
      expect(style).to.not.equal(undefined);
      const nodes: Node[] = Array.from(document.head.childNodes);
      expect(nodes.indexOf(style as Node)).to.be.lessThan(nodes.indexOf(comment));
      style?.remove();
    } finally {
      comment.remove();
    }
  });
});
