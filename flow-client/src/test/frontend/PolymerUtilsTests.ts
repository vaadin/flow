import { expect } from '@open-wc/testing';
import {
  getDomElementById,
  getDomRoot,
  getElementInShadowRootById,
  invokeWhenDefined,
  isInShadowRoot,
  isPolymerElement,
  isReady,
  mayBePolymerElement,
  searchForElementInShadowRoot
} from '../../main/frontend/internal/PolymerUtils';

describe('PolymerUtils', () => {
  it('isPolymerElement detects a Polymer 3 element and rejects a plain one', () => {
    const p3 = { constructor: { polymerElementVersion: '3.0' } } as unknown as Element;
    expect(isPolymerElement(p3)).to.be.true;
    expect(isPolymerElement(document.createElement('div'))).to.be.false;
  });

  it('mayBePolymerElement is true for custom-element tag names only', () => {
    expect(mayBePolymerElement(document.createElement('x-foo'))).to.be.true;
    expect(mayBePolymerElement(document.createElement('div'))).to.be.false;
  });

  it('isInShadowRoot distinguishes shadow and light DOM', () => {
    const host = document.createElement('div');
    const root = host.attachShadow({ mode: 'open' });
    const shadowChild = document.createElement('span');
    root.appendChild(shadowChild);
    expect(isInShadowRoot(shadowChild)).to.be.true;

    const lightChild = document.createElement('span');
    document.body.appendChild(lightChild);
    expect(isInShadowRoot(lightChild)).to.be.false;
    lightChild.remove();
  });

  it('isReady reflects presence of the Polymer local-DOM map', () => {
    expect(isReady({ $: {} } as unknown as Node)).to.be.true;
    expect(isReady({} as unknown as Node)).to.be.false;
  });

  it('getDomRoot returns the root or null', () => {
    const rootEl = document.createElement('div');
    expect(getDomRoot({ root: rootEl } as unknown as Node)).to.equal(rootEl);
    expect(getDomRoot({} as unknown as Node)).to.equal(null);
  });

  it('getDomElementById reads from the local-DOM map', () => {
    const el = document.createElement('span');
    expect(getDomElementById({ $: { foo: el } } as unknown as Node, 'foo')).to.equal(el);
    expect(getDomElementById({ $: {} } as unknown as Node, 'missing')).to.equal(null);
  });

  it('searchForElementInShadowRoot and getElementInShadowRootById query the shadow root', () => {
    const host = document.createElement('div');
    const root = host.attachShadow({ mode: 'open' });
    const child = document.createElement('span');
    child.id = 'inner';
    child.className = 'marker';
    root.appendChild(child);
    expect(searchForElementInShadowRoot(root, '.marker')).to.equal(child);
    expect(getElementInShadowRootById(root, 'inner')).to.equal(child);
    expect(searchForElementInShadowRoot(root, '.nope')).to.equal(null);
  });

  it('invokeWhenDefined runs the callback once the element is defined', async () => {
    const tag = 'x-when-defined-test';
    customElements.define(tag, class extends HTMLElement {});
    await new Promise<void>((resolve) => {
      invokeWhenDefined(tag, resolve);
    });
  });
});
