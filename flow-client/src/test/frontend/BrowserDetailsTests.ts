import { expect } from '@open-wc/testing';
import { BrowserDetails } from '../../main/frontend/internal/BrowserDetails';

// User-agent strings taken verbatim from BrowserDetailsTest.java. This is an
// essential, representative subset of that Java matrix (one case per browser
// family, engine and operating system, plus a couple of version edge cases).

const FIREFOX35_WINDOWS =
  'Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.1.8) Gecko/20100202 Firefox/3.5.8 (.NET CLR 3.5.30729) FirePHP/0.4';
const IE8_WINDOWS =
  'Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729; InfoPath.2)';
const OPERA964_WINDOWS = 'Opera/9.64(Windows NT 5.1; U; en) Presto/2.1.1';
const OPERA115_WINDOWS =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 OPR/115.0.0.0';
const CHROME4_WINDOWS =
  'Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/532.5 (KHTML, like Gecko) Chrome/4.0.249.89 Safari/532.5';
const CHROME100_WINDOWS =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4844.84 Safari/537.36';
const CHROME_40_ON_CHROMEOS =
  'Mozilla/5.0 (X11; CrOS x86_64 6457.31.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.38 Safari/537.36';
const SAFARI11_MAC =
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/11.1 Safari/605.1.15';
const IPHONE_IOS_5_1 =
  'Mozilla/5.0 (iPhone; CPU iPhone OS 5_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9B179 Safari/7534.48.3';
const ANDROID_GALAXY_NEXUS_4_0_4_CHROME =
  'Mozilla/5.0 (Linux; Android 4.0.4; Galaxy Nexus Build/IMM76B) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.133 Mobile Safari/535.19';
const EDGE_100 =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.60 Safari/537.36 Edg/100.0.1185.29';

describe('BrowserDetails', () => {
  it('detects Firefox on the Gecko engine', () => {
    const bd = new BrowserDetails(FIREFOX35_WINDOWS);
    expect(bd.isFirefox()).to.be.true;
    expect(bd.isGecko()).to.be.true;
    expect(bd.isWebKit()).to.be.false;
    expect(bd.getBrowserMajorVersion()).to.equal(3);
    expect(bd.getBrowserMinorVersion()).to.equal(5);
    expect(bd.isWindows()).to.be.true;
  });

  it('detects Chrome 4 on WebKit / Windows', () => {
    const bd = new BrowserDetails(CHROME4_WINDOWS);
    expect(bd.isChrome()).to.be.true;
    expect(bd.isWebKit()).to.be.true;
    expect(bd.getBrowserMajorVersion()).to.equal(4);
    expect(bd.getBrowserMinorVersion()).to.equal(0);
    expect(bd.getBrowserEngineVersion()).to.be.closeTo(532.5, 0.01);
    expect(bd.isWindows()).to.be.true;
  });

  it('detects Safari 11 on WebKit / Mac OS X', () => {
    const bd = new BrowserDetails(SAFARI11_MAC);
    expect(bd.isSafari()).to.be.true;
    expect(bd.isWebKit()).to.be.true;
    expect(bd.getBrowserMajorVersion()).to.equal(11);
    expect(bd.getBrowserMinorVersion()).to.equal(1);
    expect(bd.getBrowserEngineVersion()).to.be.closeTo(605.1, 0.01);
    expect(bd.isMacOSX()).to.be.true;
  });

  it('detects modern Opera as Opera on WebKit', () => {
    const bd = new BrowserDetails(OPERA115_WINDOWS);
    expect(bd.isOpera()).to.be.true;
    expect(bd.isWebKit()).to.be.true;
    expect(bd.isChrome()).to.be.false;
    expect(bd.getBrowserMajorVersion()).to.equal(115);
    expect(bd.getBrowserMinorVersion()).to.equal(0);
  });

  it('detects legacy Opera on the Presto engine', () => {
    const bd = new BrowserDetails(OPERA964_WINDOWS);
    expect(bd.isOpera()).to.be.true;
    expect(bd.isPresto()).to.be.true;
  });

  it('detects Internet Explorer on the Trident engine', () => {
    const bd = new BrowserDetails(IE8_WINDOWS);
    expect(bd.isIE()).to.be.true;
    expect(bd.isTrident()).to.be.true;
  });

  it('detects Edge (Chromium) and its version', () => {
    const bd = new BrowserDetails(EDGE_100);
    expect(bd.isEdge()).to.be.true;
    expect(bd.getBrowserMajorVersion()).to.equal(100);
    expect(bd.getBrowserMinorVersion()).to.equal(0);
  });

  it('parses the Android OS version', () => {
    const bd = new BrowserDetails(ANDROID_GALAXY_NEXUS_4_0_4_CHROME);
    expect(bd.isAndroid()).to.be.true;
    expect(bd.isChrome()).to.be.true;
    expect(bd.getOperatingSystemMajorVersion()).to.equal(4);
    expect(bd.getOperatingSystemMinorVersion()).to.equal(0);
  });

  it('parses the iOS version on iPhone', () => {
    const bd = new BrowserDetails(IPHONE_IOS_5_1);
    expect(bd.isIPhone()).to.be.true;
    expect(bd.isSafari()).to.be.true;
    expect(bd.getOperatingSystemMajorVersion()).to.equal(5);
    expect(bd.getOperatingSystemMinorVersion()).to.equal(1);
  });

  it('parses the Chrome OS version', () => {
    const bd = new BrowserDetails(CHROME_40_ON_CHROMEOS);
    expect(bd.isChromeOS()).to.be.true;
    expect(bd.getOperatingSystemMajorVersion()).to.equal(6457);
    expect(bd.getOperatingSystemMinorVersion()).to.equal(31);
  });

  it('flags unsupported browsers via isTooOldToFunctionProperly', () => {
    expect(new BrowserDetails(IE8_WINDOWS).isTooOldToFunctionProperly()).to.be.true;
    expect(new BrowserDetails(CHROME100_WINDOWS).isTooOldToFunctionProperly()).to.be.false;
  });
});
