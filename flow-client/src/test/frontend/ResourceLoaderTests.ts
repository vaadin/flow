import { expect } from '@open-wc/testing';
import {
  addOnloadHandler,
  getStyleSheetLength,
  runPromiseExpression
} from '../../main/frontend/internal/ResourceLoader';

type HandlerEl = {
  onload: (() => void) | null;
  onerror: (() => void) | null;
  onreadystatechange: (() => void) | null;
};

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

  it('getStyleSheetLength returns -1 when no matching stylesheet is loaded', () => {
    expect(getStyleSheetLength('http://example.com/does-not-exist.css')).to.equal(-1);
  });

  it('runPromiseExpression runs onSuccess when the promise resolves', async () => {
    await new Promise<void>((resolve, reject) => {
      runPromiseExpression(
        'x',
        () => Promise.resolve(),
        resolve,
        () => reject(new Error('onError called'))
      );
    });
  });

  it('runPromiseExpression runs onError when the result is not a promise', () => {
    let errored = false;
    runPromiseExpression(
      'x',
      () => 42,
      () => {},
      () => {
        errored = true;
      }
    );
    expect(errored).to.be.true;
  });

  it('runPromiseExpression runs onError when the promise rejects', async () => {
    await new Promise<void>((resolve, reject) => {
      runPromiseExpression(
        'x',
        () => Promise.reject(new Error('boom')),
        () => reject(new Error('onSuccess called')),
        resolve
      );
    });
  });
});
