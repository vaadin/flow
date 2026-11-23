// Beyond the Java suite: ReactUtils has no Java test class in src/test/java or
// src/test-gwt/java, so every case here is beyond the Java suite.

import { expect } from '@open-wc/testing';
import { addReadyCallback } from '../../../../main/frontend/internal/client/ReactUtils';

describe('ReactUtils', () => {
  it('addReadyCallback forwards to the element when supported', () => {
    let received: { name: string; callback: () => void } | undefined;
    const element = {
      addReadyCallback(name: string, callback: () => void) {
        received = { name, callback };
      }
    } as unknown as Element;
    const cb = () => {};
    addReadyCallback(element, 'container', cb);
    expect(received?.name).to.equal('container');
    expect(received?.callback).to.equal(cb);
  });

  it('addReadyCallback is a no-op when the element does not support it', () => {
    expect(() => addReadyCallback(document.createElement('div'), 'container', () => {})).to.not.throw();
  });
});
