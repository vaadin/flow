import { expect } from '@open-wc/testing';
import {
  addHtmlImportsReadyHandler,
  addOnloadHandler,
  getStyleSheetLength,
  runPromiseExpression,
  supportsHtmlWhenReady
} from '../../main/frontend/internal/ResourceLoader';

type HandlerEl = {
  onload: (() => void) | null;
  onerror: (() => void) | null;
  onreadystatechange: (() => void) | null;
};

describe('ResourceLoader', () => {
  const win = window as unknown as { HTMLImports?: unknown };
  let savedHtmlImports: unknown;
  beforeEach(() => {
    savedHtmlImports = win.HTMLImports;
  });
  afterEach(() => {
    win.HTMLImports = savedHtmlImports;
  });

  it('supportsHtmlWhenReady reflects HTMLImports.whenReady', () => {
    win.HTMLImports = undefined;
    expect(supportsHtmlWhenReady()).to.be.false;
    win.HTMLImports = { whenReady: () => {} };
    expect(supportsHtmlWhenReady()).to.be.true;
  });

  it('addHtmlImportsReadyHandler forwards to HTMLImports.whenReady', () => {
    let captured: (() => void) | undefined;
    win.HTMLImports = {
      whenReady: (cb: () => void) => {
        captured = cb;
      }
    };
    let ran = false;
    addHtmlImportsReadyHandler(() => {
      ran = true;
    });
    captured?.();
    expect(ran).to.be.true;
  });

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
