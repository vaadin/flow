import { expect } from '@open-wc/testing';
import { addReadyListener, fireReadyEvent, getCustomElement } from '../../main/frontend/internal/PolymerUtils';
import { isInitialized } from '../../main/frontend/internal/ReactUtils';

describe('PolymerUtils attach helpers', () => {
  it('addReadyListener / fireReadyEvent runs listeners once then clears them', () => {
    const element = document.createElement('div');
    const ran: string[] = [];
    addReadyListener(element, () => ran.push('a'));
    addReadyListener(element, () => ran.push('b'));
    fireReadyEvent(element);
    expect(ran.sort()).to.deep.equal(['a', 'b']);

    // A second fire is a no-op (listeners cleared).
    fireReadyEvent(element);
    expect(ran).to.have.length(2);
  });

  it('getCustomElement walks child indices, ignoring style children', () => {
    const root = document.createElement('div');
    root.appendChild(document.createElement('style'));
    const first = document.createElement('span'); // index 0 (style skipped)
    const second = document.createElement('b'); // index 1
    root.appendChild(first);
    root.appendChild(second);

    expect(getCustomElement(root, [0])).to.equal(first);
    expect(getCustomElement(root, [1])).to.equal(second);
    // Out-of-range path => null.
    expect(getCustomElement(root, [5])).to.equal(null);
  });

  it('getCustomElement descends nested paths', () => {
    const root = document.createElement('div');
    const child = document.createElement('section');
    const grandchild = document.createElement('span');
    child.appendChild(grandchild);
    root.appendChild(child);
    expect(getCustomElement(root, [0, 0])).to.equal(grandchild);
  });
});

describe('ReactUtils.isInitialized', () => {
  it('reflects whether the element lookup finds an element', () => {
    expect(isInitialized(() => document.createElement('div'))).to.be.true;
    expect(isInitialized(() => null)).to.be.false;
  });
});
