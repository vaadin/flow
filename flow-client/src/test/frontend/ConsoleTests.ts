import { expect } from '@open-wc/testing';
import { isLocalStorageFlagEnabled } from '../../main/frontend/internal/Console';

describe('Console', () => {
  const KEY = 'vaadin.browserLog';
  let saved: string | null;
  beforeEach(() => {
    saved = window.localStorage.getItem(KEY);
  });
  afterEach(() => {
    if (saved === null) {
      window.localStorage.removeItem(KEY);
    } else {
      window.localStorage.setItem(KEY, saved);
    }
  });

  it('is true only when the flag is exactly "true"', () => {
    window.localStorage.setItem(KEY, 'true');
    expect(isLocalStorageFlagEnabled()).to.be.true;

    window.localStorage.setItem(KEY, 'false');
    expect(isLocalStorageFlagEnabled()).to.be.false;

    window.localStorage.removeItem(KEY);
    expect(isLocalStorageFlagEnabled()).to.be.false;
  });
});
