import { expect } from '@open-wc/testing';
import { BrowserInfo } from '../../main/frontend/internal/BrowserInfo';

// BrowserInfo is a thin singleton wrapper over BrowserDetails, detecting the
// browser only once from the current navigator user agent. The user-agent
// parsing matrix is covered by BrowserDetailsTests; here we verify the singleton
// behavior, the touch probe, the engine-version helpers and the exported engine
// constants against the actual test-runner browser.

describe('BrowserInfo', () => {
  it('get() returns a cached singleton', () => {
    expect(BrowserInfo.get()).to.equal(BrowserInfo.get());
  });

  it('isTouchDevice returns a boolean', () => {
    expect(BrowserInfo.get().isTouchDevice()).to.be.a('boolean');
  });

  it('the browser-family probes return booleans', () => {
    const info = BrowserInfo.get();
    expect(info.isSafari()).to.be.a('boolean');
    expect(info.isSafariOrIOS()).to.be.a('boolean');
    expect(info.isChrome()).to.be.a('boolean');
    expect(info.isFirefox()).to.be.a('boolean');
    expect(info.isOpera()).to.be.a('boolean');
    expect(info.isWebkit()).to.be.a('boolean');
    expect(info.isGecko()).to.be.a('boolean');
    expect(info.isIE()).to.be.a('boolean');
    expect(info.isEdge()).to.be.a('boolean');
    expect(info.isAndroid()).to.be.a('boolean');
    expect(info.isAndroidWithBrokenScrollTop()).to.be.a('boolean');
  });

  it('engine-version helpers return -1 unless the matching engine is in use', () => {
    const info = BrowserInfo.get();
    expect(info.getGeckoVersion()).to.equal(info.isGecko() ? info.getGeckoVersion() : -1);
    expect(info.getWebkitVersion()).to.equal(info.isWebkit() ? info.getWebkitVersion() : -1);
    if (!info.isGecko()) {
      expect(info.getGeckoVersion()).to.equal(-1);
    }
    if (!info.isWebkit()) {
      expect(info.getWebkitVersion()).to.equal(-1);
    }
  });

  it('exposes the engine name constants', () => {
    expect(BrowserInfo.ENGINE_GECKO).to.equal('gecko');
    expect(BrowserInfo.ENGINE_WEBKIT).to.equal('webkit');
    expect(BrowserInfo.ENGINE_PRESTO).to.equal('presto');
    expect(BrowserInfo.ENGINE_TRIDENT).to.equal('trident');
  });
});
