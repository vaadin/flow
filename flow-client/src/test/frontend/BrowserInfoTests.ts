import { expect } from '@open-wc/testing';
import { checkForTouchDevice, getBrowserString, isIos } from '../../main/frontend/internal/BrowserInfo';

describe('BrowserInfo', () => {
  it('getBrowserString returns the navigator user agent', () => {
    expect(getBrowserString()).to.equal(navigator.userAgent);
  });

  it('checkForTouchDevice returns a boolean', () => {
    expect(checkForTouchDevice()).to.be.a('boolean');
  });

  it('isIos returns a boolean', () => {
    expect(isIos()).to.be.a('boolean');
  });
});
