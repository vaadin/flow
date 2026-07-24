import { expect } from '@open-wc/testing';
import {
  checkForTouchDevice,
  getBrowserString,
  isIos,
  isOpera,
  isSafari,
  isWebkit
} from '../../main/frontend/internal/BrowserInfo';

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

  describe('browser-family probes (by user agent)', () => {
    const original = navigator.userAgent;
    const setUserAgent = (ua: string) =>
      Object.defineProperty(navigator, 'userAgent', { value: ua, configurable: true });
    afterEach(() => Object.defineProperty(navigator, 'userAgent', { value: original, configurable: true }));

    const CHROME =
      'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36';
    const SAFARI =
      'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15';
    const OPERA =
      'Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36 OPR/106.0';
    const FIREFOX = 'Mozilla/5.0 (Windows NT 10.0; rv:121.0) Gecko/20100101 Firefox/121.0';

    it('detects Safari but not Chrome/Opera as Safari', () => {
      setUserAgent(SAFARI);
      expect(isSafari()).to.be.true;
      setUserAgent(CHROME);
      expect(isSafari()).to.be.false;
      setUserAgent(OPERA);
      expect(isSafari()).to.be.false;
    });

    it('detects Opera', () => {
      setUserAgent(OPERA);
      expect(isOpera()).to.be.true;
      setUserAgent(CHROME);
      expect(isOpera()).to.be.false;
    });

    it('detects WebKit (Chrome/Safari) but not Firefox', () => {
      setUserAgent(CHROME);
      expect(isWebkit()).to.be.true;
      setUserAgent(SAFARI);
      expect(isWebkit()).to.be.true;
      setUserAgent(FIREFOX);
      expect(isWebkit()).to.be.false;
    });
  });
});
