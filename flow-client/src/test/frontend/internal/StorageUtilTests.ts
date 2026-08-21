import { expect } from '@open-wc/testing';
import {
  getLocalItem,
  getSessionItem,
  setLocalItem,
  setSessionItem
} from '../../../main/frontend/internal/StorageUtil';

describe('StorageUtil', () => {
  // Every case here is beyond the Java suite: com.vaadin.client.StorageUtil has no Java test,
  // so there is nothing to port 1:1 and all coverage below is additional
  // (PORTING.md rule 13.6).
  const key = 'flow-storage-util-test';
  afterEach(() => {
    window.localStorage.removeItem(key);
    window.sessionStorage.removeItem(key);
  });

  it('round-trips a value through local storage', () => {
    expect(getLocalItem(key)).to.equal(null);
    setLocalItem(key, 'local-value');
    expect(getLocalItem(key)).to.equal('local-value');
    expect(window.localStorage.getItem(key)).to.equal('local-value');
  });

  it('round-trips a value through session storage', () => {
    expect(getSessionItem(key)).to.equal(null);
    setSessionItem(key, 'session-value');
    expect(getSessionItem(key)).to.equal('session-value');
    expect(window.sessionStorage.getItem(key)).to.equal('session-value');
  });

  it('keeps local and session storage independent', () => {
    setLocalItem(key, 'local-only');
    expect(getSessionItem(key)).to.equal(null);
    setSessionItem(key, 'session-only');
    expect(getLocalItem(key)).to.equal('local-only');
  });
});
