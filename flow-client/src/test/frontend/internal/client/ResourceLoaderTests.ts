import { expect } from '@open-wc/testing';
import { testRegistry } from './testRegistry';
import { ResourceLoader, addOnloadHandler } from '../../../../main/frontend/internal/client/ResourceLoader';
import type { ResourceLoadListener } from '../../../../main/frontend/internal/client/ResourceRegistry';

type HandlerEl = {
  onload: (() => void) | null;
  onerror: (() => void) | null;
  onreadystatechange: (() => void) | null;
};

// Beyond the Java suite: com.vaadin.client.ResourceLoader has no test class of
// its own.
const settle = (ms = 50) => new Promise((resolve) => setTimeout(resolve, ms));

function recordingListener() {
  const calls: string[] = [];
  return { calls, listener: { onLoad: () => calls.push('load'), onError: () => calls.push('error') } };
}

describe('ResourceLoader', () => {
  it('addOnloadHandler calls onLoad and clears the handlers', () => {
    const el = document.createElement('script') as unknown as HandlerEl;
    let loaded = false;
    let errored = false;
    addOnloadHandler(
      el as unknown as Element,
      () => {
        loaded = true;
      },
      () => {
        errored = true;
      }
    );
    expect(el.onload).to.be.a('function');
    el.onload?.();
    expect(loaded).to.be.true;
    expect(errored).to.be.false;
    expect(el.onload).to.equal(null);
    expect(el.onerror).to.equal(null);
  });

  it('addOnloadHandler calls onError on error', () => {
    const el = document.createElement('script') as unknown as HandlerEl;
    let errored = false;
    addOnloadHandler(
      el as unknown as Element,
      () => {},
      () => {
        errored = true;
      }
    );
    el.onerror?.();
    expect(errored).to.be.true;
    expect(el.onerror).to.equal(null);
  });

  // runPromiseExpression is private in Java; it is reached through the public
  // loadDynamicImport, which resolves the listener from the promise it returns.
  describe('loadDynamicImport', () => {
    const loader = () => new ResourceLoader(testRegistry({ SystemErrorHandler: { handleError: () => {} } }), false);
    const listener = (onLoad: () => void, onError: () => void): ResourceLoadListener => ({
      onLoad,
      onError
    });

    it('notifies onLoad when the expression resolves', async () => {
      await new Promise<void>((resolve, reject) => {
        loader().loadDynamicImport(
          'return Promise.resolve()',
          listener(resolve, () => reject(new Error('onError called')))
        );
      });
    });

    it('notifies onError when the expression does not return a promise', () => {
      let errored = false;
      loader().loadDynamicImport(
        'return 42',
        listener(
          () => {},
          () => {
            errored = true;
          }
        )
      );
      expect(errored).to.be.true;
    });

    it('notifies onError when the promise rejects', async () => {
      await new Promise<void>((resolve, reject) => {
        loader().loadDynamicImport(
          'return Promise.reject(new Error("nope"))',
          listener(() => reject(new Error('onLoad called')), resolve)
        );
      });
    });
  });

  it('loads an external script and dedupes a repeat request', async () => {
    const loader = new ResourceLoader(testRegistry({ SystemErrorHandler: { handleError: () => {} } }), false);
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

  it('loads a stylesheet (in head, before the marker comment) and dedupes', async () => {
    const comment = document.createComment('Stylesheet end');
    document.head.appendChild(comment);
    try {
      const loader = new ResourceLoader(testRegistry({ SystemErrorHandler: { handleError: () => {} } }), false);
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

  it('inserts an inline stylesheet before the marker comment', () => {
    const comment = document.createComment('Stylesheet end');
    document.head.appendChild(comment);
    try {
      const loader = new ResourceLoader(testRegistry({ SystemErrorHandler: { handleError: () => {} } }), false);
      const css = '.inline-probe{color:blue}';
      // A <style> element fires no load event, so this covers the insertion
      // position only; notification is covered by the loadStylesheet case above.
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
