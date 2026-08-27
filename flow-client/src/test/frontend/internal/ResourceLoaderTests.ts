import { expect } from '@open-wc/testing';
import { ResourceLoader, addOnloadHandler } from '../../../main/frontend/internal/client/ResourceLoader';
import type { ResourceLoadListener } from '../../../main/frontend/internal/client/ResourceRegistry';

type HandlerEl = {
  onload: (() => void) | null;
  onerror: (() => void) | null;
  onreadystatechange: (() => void) | null;
};

// Beyond the Java suite: com.vaadin.client.ResourceLoader has no test class of
// its own.
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
    const loader = () => new ResourceLoader({ handleError: () => {} }, false);
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
});
