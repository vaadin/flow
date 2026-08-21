import { expect } from '@open-wc/testing';
import { BrowserDetails } from '../../../../main/frontend/internal/shared/BrowserDetails';

// User-agent strings and expected values taken verbatim from
// BrowserDetailsTest.java. Every @Test case from the Java suite is ported here,
// including the two data-driven suites (testCommonDesktopUserAgents /
// testMobileUserAgents), whose JSON fixtures are inlined below.

const FIREFOX30_WINDOWS = 'Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.0.6) Gecko/2009011913 Firefox/3.0.6';
const FIREFOX30_LINUX =
  'Mozilla/5.0 (X11; U; Linux x86_64; es-ES; rv:1.9.0.12) Gecko/2009070811 Ubuntu/9.04 (jaunty) Firefox/3.0.12';
const FIREFOX33_ANDROID = 'Mozilla/5.0 (Android; Tablet; rv:33.0) Gecko/33.0 Firefox/33.0';
const FIREFOX35_WINDOWS =
  'Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.1.8) Gecko/20100202 Firefox/3.5.8 (.NET CLR 3.5.30729) FirePHP/0.4';
const FIREFOX36_WINDOWS =
  'Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2) Gecko/20100115 Firefox/3.6 (.NET CLR 3.5.30729)';
const FIREFOX36B_MAC =
  'UAString mozilla/5.0 (macintosh; u; intel mac os x 10.6; en-us; rv:1.9.2) gecko/20100115 firefox/3.6';
const FIREFOX_30B5_MAC =
  'Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9b5) Gecko/2008032619 Firefox/3.0b5';
const FIREFOX_40B7_WIN = 'Mozilla/5.0 (Windows NT 5.1; rv:2.0b7) Gecko/20100101 Firefox/4.0b7';
const FIREFOX_40B11_WIN = 'Mozilla/5.0 (Windows NT 5.1; rv:2.0b11) Gecko/20100101 Firefox/4.0b11';
const KONQUEROR_LINUX = 'Mozilla/5.0 (compatible; Konqueror/3.5; Linux) KHTML/3.5.5 (like Gecko) (Exabot-Thumbnails)';

const IE6_WINDOWS = 'Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 2.0.50727)';
const IE7_WINDOWS =
  'Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; .NET CLR 2.0.50727; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)';
const IE8_WINDOWS =
  'Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729; InfoPath.2)';
const IE9_IN_IE7_MODE_WINDOWS_7 =
  'Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.1; Trident/5.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C)';
const IE9_BETA_IN_IE8_MODE_WINDOWS_7 =
  'Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; Trident/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C)';
const IE9_BETA_WINDOWS_7 = 'Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)';
const IE10_WINDOWS_8 = 'Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; WOW64; Trident/6.0)';
const IE11_WINDOWS_7 =
  'Mozilla/5.0 (Windows NT 6.1; Trident/7.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; rv:11.0) like Gecko';
const IE11_IN_IE7_MODE_WINDOWS_7 =
  'Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.1; WOW64; Trident/7.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E)';
const IE11_IN_IE7_MODE_WINDOWS_10 =
  'Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 10.0; WOW64; Trident/7.0; .NET4.0C; .NET4.0E)';
const IE11_IN_IE7_MODE_LAUNCH_DAY_WINDOWS_10 =
  'Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 10.0; WOW64; Trident/8.0; .NET4.0C; .NET4.0E)';
const IE11_WINDOWS_PHONE_8_1_UPDATE =
  'Mozilla/5.0 (Mobile; Windows Phone 8.1; Android 4.0; ARM; Trident/7.0; Touch; rv:11.0; IEMobile/11.0; NOKIA; Lumia 920) Like iPhone OS 7_0_3 Mac OS X AppleWebKit/537 (KHTML, like Gecko) Mobile Safari/537';

const OPERA964_WINDOWS = 'Opera/9.64(Windows NT 5.1; U; en) Presto/2.1.1';
const OPERA1010_WINDOWS = 'Opera/9.80 (Windows NT 5.1; U; en) Presto/2.2.15 Version/10.10';
const OPERA1050_WINDOWS = 'Opera/9.80 (Windows NT 5.1; U; en) Presto/2.5.22 Version/10.50';
const OPERA115_WINDOWS =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 OPR/115.0.0.0';

const CHROME3_MAC =
  'Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_5_8; en-US) AppleWebKit/532.0 (KHTML, like Gecko) Chrome/3.0.198 Safari/532.0';
const CHROME4_WINDOWS =
  'Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/532.5 (KHTML, like Gecko) Chrome/4.0.249.89 Safari/532.5';
const CHROME_IOS =
  'Mozilla/5.0 (iPhone; CPU iPhone OS 9_2_1 like Mac OS X) AppleWebKit/601.1 (KHTML, like Gecko) CriOS/49.0.2623.73 Mobile/13D15 Safari/601.1.46';
const CHROME_40_ON_CHROMEOS =
  'Mozilla/5.0 (X11; CrOS x86_64 6457.31.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.38 Safari/537.36';
const CHROME_IOS_DESKTOP =
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_5) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/85 Version/11.1.1 Safari/605.1.15';

const SAFARI3_WINDOWS =
  'Mozilla/5.0 (Windows; U; Windows NT 5.1; cs-CZ) AppleWebKit/525.28.3 (KHTML, like Gecko) Version/3.2.3 Safari/525.29';
const SAFARI4_MAC =
  'Mozilla/5.0 (Macintosh; U; PPC Mac OS X 10_5_8; en-us) AppleWebKit/531.22.7 (KHTML, like Gecko) Version/4.0.5 Safari/531.22.7';
const SAFARI10_WINDOWS =
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/603.3.8 (KHTML, like Gecko) Version/10.1.2 Safari/603.3.8';
const SAFARI11_MAC =
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/11.1 Safari/605.1.15';

const IPHONE_IOS_5_1 =
  'Mozilla/5.0 (iPhone; CPU iPhone OS 5_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9B179 Safari/7534.48.3';
const IPHONE_IOS_4_0 =
  'Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_0 like Mac OS X; en-us) AppleWebKit/532.9 (KHTML, like Gecko) Version/4.0.5 Mobile/8A293 Safari/6531.22.7';
const IPAD_IOS_4_3_1 =
  'Mozilla/5.0 (iPad; U; CPU OS 4_3_1 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8G4 Safari/6533.18.5';

// application on the home screen, without Safari in user agent
const IPHONE_IOS_6_1_HOMESCREEN_SIMULATOR =
  'Mozilla/5.0 (iPhone; CPU iPhone OS 6_1 like Mac OS X) AppleWebKit/536.26 (KHTML, like Gecko) Mobile/10B141';

const ANDROID_HTC_2_1 =
  'Mozilla/5.0 (Linux; U; Android 2.1-update1; en-us; ADR6300 Build/ERE27) AppleWebKit/530.17 (KHTML, like Gecko) Version/4.0 Mobile Safari/530.17';
const ANDROID_GOOGLE_NEXUS_2_2 =
  'Mozilla/5.0 (Linux; U; Android 2.2; en-us; Nexus One Build/FRF91) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1';
const ANDROID_MOTOROLA_3_0 =
  'Mozilla/5.0 (Linux; U; Android 3.0; en-us; Xoom Build/HRI39) AppleWebKit/534.13 (KHTML, like Gecko) Version/4.0 Safari/534.13';
const ANDROID_GALAXY_NEXUS_4_0_4_CHROME =
  'Mozilla/5.0 (Linux; Android 4.0.4; Galaxy Nexus Build/IMM76B) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.133 Mobile Safari/535.19';
const ANDROID_CALLPOD_KEEPER =
  'callpod keeper for android 1.0 (10.1.1/240) dalvik/2.1.0 (linux; u; android 6.0; lg-v495 build/mra58k)';

const EDGE_12_WINDOWS_10 =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/12.10240';

const ECLIPSE_MAC_SAFARI_91 =
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_4) AppleWebKit/601.5.17 (KHTML, like Gecko) Safari/522.0';
const ECLIPSE_MAC_SAFARI_90 =
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_4) AppleWebKit/601.4.4 (KHTML, like Gecko) Safari/522.0';

const IPHONE_IOS_11_FACEBOOK_BROWSER =
  'Mozilla/5.0 (iPhone; CPU iPhone OS 11_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E302 [FBAN/MessengerForiOS;FBAV/165.0.0.45.95;FBBV/107115338;FBDV/iPhone10,6;FBMD/iPhone;FBSN/iOS;FBSV/11.3.1;FBSS/3;FBCR/DNA;FBID/phone;FBLC/en_GB;FBOP/5;FBRV/0]';
const IPHONE_IOS_11_FIREFOX =
  'Mozilla/5.0 (iPhone; CPU iPhone OS 11_1_2 like Mac OS X) AppleWebKit/604.3.5 (KHTML, like Gecko) FxiOS/11.1b10377 Mobile/15B202 Safari/604.3.5';

const EDGE_100 =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.60 Safari/537.36 Edg/100.0.1185.29';
const EDGE_99_MAC =
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 12_3_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.75 Safari/537.36 Edg/99.0.1150.36';
const EDGE_97_ANDROID =
  'Mozilla/5.0 (Linux; Android 10; Pixel 3 XL) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.79 Mobile Safari/537.36 EdgA/97.0.1072.69';
const EDGE_97_IOS =
  'Mozilla/5.0 (iPhone; CPU iPhone OS 15_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 EdgiOS/97.1072.69 Mobile/15E148 Safari/605.1.15';

// Version 100 Strings
const CHROME100_WINDOWS =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4844.84 Safari/537.36';
const FIREFOX_100_WIN64 = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:100.0) Gecko/20100101 Firefox/100.0';
const FIREFOX_100_WIN32 = 'Mozilla/5.0 (Windows NT 10.0; rv:100.0) Gecko/20100101 Firefox/100.0';
const FIREFOX_100_MACOS = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:100.0) Gecko/20100101 Firefox/100.0';
const FIREFOX_100_LINUX = 'Mozilla/5.0 (X11; Linux x86_64; rv:100.0) Gecko/20100101 Firefox/100.0';

// Web crawlers and bots
const BYTE_SPIDER =
  'mozilla/5.0 (linux; android 5.0) applewebkit/537.36 (khtml, like gecko) mobile safari/537.36 (compatible; bytespider; spider-feedback@bytedance.com)';
const DUCK_DUCK_BOT = 'ddg_android/5.169.0 (com.duckduckgo.mobile.android; android api 33)';
const DUCK_DUCK_BOT_3 = 'DuckDuckGo/0.26.3 CFNetwork/1331.0.7 Darwin/21.4.0';
const DUCK_DUCK_BOT_2 =
  'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/130.0.6723.106 Mobile DuckDuckGo/5 Safari/537.36';

// Engine assertions compare a float in Java; a small tolerance keeps the
// parsed value robust while still matching to two decimals.
function assertEngineVersion(bd: BrowserDetails, version: number): void {
  expect(bd.getBrowserEngineVersion()).to.be.closeTo(version, 0.01);
}

function assertBrowserMajorVersion(bd: BrowserDetails, version: number): void {
  expect(bd.getBrowserMajorVersion()).to.equal(version);
}

function assertBrowserMinorVersion(bd: BrowserDetails, version: number): void {
  expect(bd.getBrowserMinorVersion()).to.equal(version);
}

function assertOSMajorVersion(bd: BrowserDetails, version: number): void {
  expect(bd.getOperatingSystemMajorVersion()).to.equal(version);
}

function assertOSMinorVersion(bd: BrowserDetails, version: number): void {
  expect(bd.getOperatingSystemMinorVersion()).to.equal(version);
}

function assertGecko(bd: BrowserDetails): void {
  expect(bd.isGecko()).to.be.true;
  expect(bd.isWebKit()).to.be.false;
  expect(bd.isPresto()).to.be.false;
  expect(bd.isTrident()).to.be.false;
}

function assertPresto(bd: BrowserDetails): void {
  expect(bd.isGecko()).to.be.false;
  expect(bd.isWebKit()).to.be.false;
  expect(bd.isPresto()).to.be.true;
  expect(bd.isTrident()).to.be.false;
}

function assertTrident(bd: BrowserDetails): void {
  expect(bd.isGecko()).to.be.false;
  expect(bd.isWebKit()).to.be.false;
  expect(bd.isPresto()).to.be.false;
  expect(bd.isTrident()).to.be.true;
}

function assertWebKit(bd: BrowserDetails): void {
  expect(bd.isGecko()).to.be.false;
  expect(bd.isWebKit()).to.be.true;
  expect(bd.isPresto()).to.be.false;
  expect(bd.isTrident()).to.be.false;
}

function assertFirefox(bd: BrowserDetails): void {
  expect(bd.isFirefox()).to.be.true;
  expect(bd.isChrome()).to.be.false;
  expect(bd.isIE()).to.be.false;
  expect(bd.isOpera()).to.be.false;
  expect(bd.isSafari()).to.be.false;
  expect(bd.isEdge()).to.be.false;
}

function assertChrome(bd: BrowserDetails): void {
  expect(bd.isFirefox()).to.be.false;
  expect(bd.isChrome()).to.be.true;
  expect(bd.isIE()).to.be.false;
  expect(bd.isOpera()).to.be.false;
  expect(bd.isSafari()).to.be.false;
  expect(bd.isEdge()).to.be.false;
}

function assertIE(bd: BrowserDetails): void {
  expect(bd.isFirefox()).to.be.false;
  expect(bd.isChrome()).to.be.false;
  expect(bd.isIE()).to.be.true;
  expect(bd.isOpera()).to.be.false;
  expect(bd.isSafari()).to.be.false;
  expect(bd.isEdge()).to.be.false;
}

function assertOpera(bd: BrowserDetails): void {
  expect(bd.isFirefox()).to.be.false;
  expect(bd.isChrome()).to.be.false;
  expect(bd.isIE()).to.be.false;
  expect(bd.isOpera()).to.be.true;
  expect(bd.isSafari()).to.be.false;
  expect(bd.isEdge()).to.be.false;
}

function assertSafari(bd: BrowserDetails): void {
  expect(bd.isFirefox()).to.be.false;
  expect(bd.isChrome()).to.be.false;
  expect(bd.isIE()).to.be.false;
  expect(bd.isOpera()).to.be.false;
  expect(bd.isSafari()).to.be.true;
  expect(bd.isEdge()).to.be.false;
}

function assertEdge(bd: BrowserDetails): void {
  expect(bd.isFirefox()).to.be.false;
  expect(bd.isChrome()).to.be.false;
  expect(bd.isIE()).to.be.false;
  expect(bd.isOpera()).to.be.false;
  expect(bd.isSafari()).to.be.false;
  expect(bd.isEdge()).to.be.true;
}

function assertUnspecifiedBrowser(bd: BrowserDetails): void {
  expect(bd.isFirefox()).to.be.false;
  expect(bd.isChrome()).to.be.false;
  expect(bd.isIE()).to.be.false;
  expect(bd.isOpera()).to.be.false;
  expect(bd.isSafari()).to.be.false;
  expect(bd.isEdge()).to.be.false;
}

function assertMacOSX(bd: BrowserDetails): void {
  expect(bd.isLinux()).to.be.false;
  expect(bd.isWindows()).to.be.false;
  expect(bd.isMacOSX()).to.be.true;
  expect(bd.isAndroid()).to.be.false;
  expect(bd.isChromeOS()).to.be.false;
}

function assertAndroid(bd: BrowserDetails): void {
  expect(bd.isLinux()).to.be.false;
  expect(bd.isWindows()).to.be.false;
  expect(bd.isMacOSX()).to.be.false;
  expect(bd.isAndroid()).to.be.true;
  expect(bd.isChromeOS()).to.be.false;
}

function assertAndroidVersion(bd: BrowserDetails, majorVersion: number, minorVersion: number): void {
  assertAndroid(bd);
  assertOSMajorVersion(bd, majorVersion);
  assertOSMinorVersion(bd, minorVersion);
}

function assertIPhone(bd: BrowserDetails): void {
  expect(bd.isIPhone()).to.be.true;
}

function assertIPad(bd: BrowserDetails): void {
  expect(bd.isIPad()).to.be.true;
}

function assertWindows(bd: BrowserDetails, isWindowsPhone = false): void {
  expect(bd.isLinux()).to.be.false;
  expect(bd.isWindows()).to.be.true;
  expect(bd.isMacOSX()).to.be.false;
  expect(bd.isAndroid()).to.be.false;
  expect(bd.isChromeOS()).to.be.false;
  expect(bd.isWindowsPhone()).to.equal(isWindowsPhone);
}

function assertLinux(bd: BrowserDetails): void {
  expect(bd.isLinux()).to.be.true;
  expect(bd.isWindows()).to.be.false;
  expect(bd.isMacOSX()).to.be.false;
  expect(bd.isAndroid()).to.be.false;
  expect(bd.isChromeOS()).to.be.false;
}

function assertChromeOS(bd: BrowserDetails, majorVersion: number, minorVersion: number): void {
  expect(bd.isLinux()).to.be.false;
  expect(bd.isWindows()).to.be.false;
  expect(bd.isMacOSX()).to.be.false;
  expect(bd.isAndroid()).to.be.false;
  expect(bd.isChromeOS()).to.be.true;
  assertOSMajorVersion(bd, majorVersion);
  assertOSMinorVersion(bd, minorVersion);
}

function assertOs(bd: BrowserDetails, os: string): void {
  switch (os) {
    case 'LINUX':
      assertLinux(bd);
      break;
    case 'WINDOWS':
      assertWindows(bd);
      break;
    case 'MACOSX':
      assertMacOSX(bd);
      break;
    case 'IPAD':
      assertIPad(bd);
      break;
    case 'IPHONE':
      assertIPhone(bd);
      break;
    case 'ANDROID':
      assertAndroid(bd);
      break;
    default:
      throw new Error(`${os} is not a supported OS`);
  }
}

interface UserAgent {
  ua: string;
  browser: string;
  browserVersion: string;
  os: string;
  device?: string;
}

function getMinorMajorVersion(browserVersion: string): { major: number; minor: number } {
  const digits = browserVersion.split(/[-.]/);
  const major = parseInt(digits[0], 10);
  let minor = -1;
  if (digits.length >= 2) {
    minor = parseInt(digits[1], 10);
  }
  return { major, minor };
}

function assertAgentDetails(agents: UserAgent[]): void {
  for (const agent of agents) {
    const bd = new BrowserDetails(agent.ua);
    assertOs(bd, agent.os);
    const versions = getMinorMajorVersion(agent.browserVersion);
    expect(bd.getBrowserMajorVersion(), `Major version differs on userAgent ${agent.ua}`).to.equal(versions.major);
    expect(bd.getBrowserMinorVersion(), `Minor version differs on userAgent ${agent.ua}`).to.equal(versions.minor);
  }
}

// Inlined from flow-server test resource common-desktop-useragents.json.
const COMMON_DESKTOP_USER_AGENTS: UserAgent[] = [
  {
    ua: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Safari/605.1.1',
    browser: 'Safari',
    browserVersion: '17.6',
    os: 'MACOSX'
  },
  {
    ua: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.3',
    browser: 'Chrome',
    browserVersion: '113.0.0',
    os: 'MACOSX'
  },
  {
    ua: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.3',
    browser: 'Chrome',
    browserVersion: '130.0.0',
    os: 'WINDOWS'
  },
  {
    ua: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.3',
    browser: 'Chrome',
    browserVersion: '130.0.0',
    os: 'MACOSX'
  },
  {
    ua: 'Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0',
    browser: 'Firefox',
    browserVersion: '115.0',
    os: 'WINDOWS'
  },
  {
    ua: 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.3',
    browser: 'Chrome',
    browserVersion: '130.0.0',
    os: 'LINUX'
  },
  {
    ua: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:132.0) Gecko/20100101 Firefox/132.0',
    browser: 'Firefox',
    browserVersion: '132.0',
    os: 'WINDOWS'
  },
  {
    ua: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36 Edge/18.1958',
    browser: 'Edge',
    browserVersion: '18.1958',
    os: 'WINDOWS'
  },
  {
    ua: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/117.0',
    browser: 'Firefox',
    browserVersion: '117.0',
    os: 'WINDOWS'
  },
  {
    ua: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 OPR/114.0.0.',
    browser: 'Opera',
    browserVersion: '114.0.0',
    os: 'WINDOWS'
  }
];

// Inlined from flow-server test resource mobile-useragents.json.
const MOBILE_USER_AGENTS: UserAgent[] = [
  {
    ua: 'Mozilla/5.0 (iPad; CPU OS 14_7_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) FxiOS/132.0 Mobile/15E148 Safari/605.1.15',
    browser: 'Firefox',
    browserVersion: '132.0',
    os: 'IPAD'
  },
  {
    ua: 'Mozilla/5.0 (iPad; CPU OS 17_7_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1',
    browser: 'Safari',
    browserVersion: '18.0',
    os: 'IPAD'
  },
  {
    ua: 'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.6723.102 Mobile Safari/537.36',
    browser: 'Chrome',
    browserVersion: '130.0.6723.102',
    os: 'ANDROID',
    device: 'K'
  },
  {
    ua: 'Mozilla/5.0 (Android 15; Mobile; rv:132.0) Gecko/132.0 Firefox/132.0',
    browser: 'Firefox',
    browserVersion: '132.0',
    os: 'ANDROID',
    device: 'Generic android'
  },
  {
    ua: 'Mozilla/5.0 (Linux; Android 10; VOG-L29) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.6723.102 Mobile Safari/537.36 OPR/76.2.4027.73374',
    browser: 'Opera',
    browserVersion: '76.2.4027.73374',
    os: 'ANDROID',
    device: 'Huawei'
  },
  {
    ua: 'Mozilla/5.0 (Linux; Android 10; SM-G970F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.6723.102 Mobile Safari/537.36 OPR/76.2.4027.73374',
    browser: 'Opera',
    browserVersion: '76.2.4027.73374',
    os: 'ANDROID',
    device: 'Samsung'
  },
  {
    ua: 'Mozilla/5.0 (Linux; Android 10; SM-N975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.6723.102 Mobile Safari/537.36 OPR/76.2.4027.73374',
    browser: 'Opera',
    browserVersion: '76.2.4027.73374',
    os: 'ANDROID',
    device: 'Samsung'
  },
  {
    ua: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/131.0.6778.31 Mobile/15E148 Safari/604.1',
    browser: 'Chrome',
    browserVersion: '131.0.6778',
    os: 'IPHONE'
  },
  {
    ua: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_7_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 EdgiOS/130.2849.68 Mobile/15E148 Safari/605.1.15',
    browser: 'Edge',
    browserVersion: '130.2849.68',
    os: 'IPHONE'
  },
  {
    ua: 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_7_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) FxiOS/132.0 Mobile/15E148 Safari/605.1.15',
    browser: 'Firefox',
    browserVersion: '132.0',
    os: 'IPHONE'
  },
  {
    ua: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_7_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1',
    browser: 'Safari',
    browserVersion: '18.0',
    os: 'IPHONE'
  }
];

describe('BrowserDetails', () => {
  it('testSafari3', () => {
    const bd = new BrowserDetails(SAFARI3_WINDOWS);
    assertWebKit(bd);
    assertSafari(bd);
    assertBrowserMajorVersion(bd, 3);
    assertBrowserMinorVersion(bd, 2);
    assertEngineVersion(bd, 525.28);
    assertWindows(bd);
  });

  it('testSafari4', () => {
    const bd = new BrowserDetails(SAFARI4_MAC);
    assertWebKit(bd);
    assertSafari(bd);
    assertBrowserMajorVersion(bd, 4);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 531.22);
    assertMacOSX(bd);
  });

  it('testSafari10', () => {
    const bd = new BrowserDetails(SAFARI10_WINDOWS);
    assertWebKit(bd);
    assertSafari(bd);
    assertBrowserMajorVersion(bd, 10);
    assertBrowserMinorVersion(bd, 1);
    assertEngineVersion(bd, 603.3);
    assertMacOSX(bd);
  });

  it('testSafari11', () => {
    const bd = new BrowserDetails(SAFARI11_MAC);
    assertWebKit(bd);
    assertSafari(bd);
    assertBrowserMajorVersion(bd, 11);
    assertBrowserMinorVersion(bd, 1);
    assertEngineVersion(bd, 605.1);
    assertMacOSX(bd);
  });

  it('testIPhoneIOS6Homescreen', () => {
    const bd = new BrowserDetails(IPHONE_IOS_6_1_HOMESCREEN_SIMULATOR);
    assertWebKit(bd);
    // not identified as Safari, no browser version available
    assertEngineVersion(bd, 536.26);
    assertIPhone(bd);
  });

  it('testIPhoneIOS5', () => {
    const bd = new BrowserDetails(IPHONE_IOS_5_1);
    assertWebKit(bd);
    assertSafari(bd);
    assertBrowserMajorVersion(bd, 5);
    assertBrowserMinorVersion(bd, 1);
    assertEngineVersion(bd, 534.46);
    assertIPhone(bd);
  });

  it('testIPhoneIOS4', () => {
    const bd = new BrowserDetails(IPHONE_IOS_4_0);
    assertWebKit(bd);
    assertSafari(bd);
    assertBrowserMajorVersion(bd, 4);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 532.9);
    assertIPhone(bd);
  });

  it('testIPadIOS4', () => {
    const bd = new BrowserDetails(IPAD_IOS_4_3_1);
    assertWebKit(bd);
    assertSafari(bd);
    assertBrowserMajorVersion(bd, 5);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 533.17);
  });

  it('testAndroid21', () => {
    const bd = new BrowserDetails(ANDROID_HTC_2_1);
    assertWebKit(bd);
    assertSafari(bd);
    assertBrowserMajorVersion(bd, 4);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 530.17);
    assertAndroidVersion(bd, 2, 1);
  });

  it('testAndroid22', () => {
    const bd = new BrowserDetails(ANDROID_GOOGLE_NEXUS_2_2);
    assertWebKit(bd);
    assertSafari(bd);
    assertBrowserMajorVersion(bd, 4);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 533.1);
    assertAndroidVersion(bd, 2, 2);
  });

  it('testAndroid30', () => {
    const bd = new BrowserDetails(ANDROID_MOTOROLA_3_0);
    assertWebKit(bd);
    assertSafari(bd);
    assertBrowserMajorVersion(bd, 4);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 534.13);
    assertAndroidVersion(bd, 3, 0);
  });

  it('testAndroid40Chrome', () => {
    const bd = new BrowserDetails(ANDROID_GALAXY_NEXUS_4_0_4_CHROME);
    assertWebKit(bd);
    assertChrome(bd);
    assertBrowserMajorVersion(bd, 18);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 535.19);
    assertAndroidVersion(bd, 4, 0);
  });

  it('testAndroidCallpodKeeper', () => {
    const bd = new BrowserDetails(ANDROID_CALLPOD_KEEPER);
    assertOSMajorVersion(bd, 6);
    assertOSMinorVersion(bd, 0);
    assertEngineVersion(bd, -1);
  });

  it('testChrome3', () => {
    const bd = new BrowserDetails(CHROME3_MAC);
    assertWebKit(bd);
    assertChrome(bd);
    assertBrowserMajorVersion(bd, 3);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 532.0);
    assertMacOSX(bd);
  });

  it('testChrome4', () => {
    const bd = new BrowserDetails(CHROME4_WINDOWS);
    assertWebKit(bd);
    assertChrome(bd);
    assertBrowserMajorVersion(bd, 4);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 532.5);
    assertWindows(bd);
  });

  it('testChromeIOS', () => {
    const bd = new BrowserDetails(CHROME_IOS);
    assertWebKit(bd);
    assertChrome(bd);
    assertBrowserMajorVersion(bd, 49);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 601.1);
  });

  it('testChromeIOSDesktopSiteFeature', () => {
    const bd = new BrowserDetails(CHROME_IOS_DESKTOP);
    assertWebKit(bd);
    assertChrome(bd);
    assertBrowserMajorVersion(bd, 85);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 605.1);
  });

  it('testChromeChromeOS', () => {
    const bd = new BrowserDetails(CHROME_40_ON_CHROMEOS);
    assertWebKit(bd);
    assertChrome(bd);
    assertBrowserMajorVersion(bd, 40);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 537.36);
    assertChromeOS(bd, 6457, 31);
  });

  it('testChrome100Windows', () => {
    const bd = new BrowserDetails(CHROME100_WINDOWS);
    assertWebKit(bd);
    assertChrome(bd);
    assertBrowserMajorVersion(bd, 100);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 537.36);
    assertWindows(bd);
  });

  it('testFirefox100Windows', () => {
    const bd = new BrowserDetails(FIREFOX_100_WIN64);
    assertGecko(bd);
    assertFirefox(bd);
    assertBrowserMajorVersion(bd, 100);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 100.0);
    assertWindows(bd);
  });

  it('testFirefox100Windows32', () => {
    const bd = new BrowserDetails(FIREFOX_100_WIN32);
    assertGecko(bd);
    assertFirefox(bd);
    assertBrowserMajorVersion(bd, 100);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 100.0);
    assertWindows(bd);
  });

  it('testFirefox100MacOs', () => {
    const bd = new BrowserDetails(FIREFOX_100_MACOS);
    assertGecko(bd);
    assertFirefox(bd);
    assertBrowserMajorVersion(bd, 100);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 100.0);
    assertMacOSX(bd);
  });

  it('testFirefox100Linux', () => {
    const bd = new BrowserDetails(FIREFOX_100_LINUX);
    assertGecko(bd);
    assertFirefox(bd);
    assertBrowserMajorVersion(bd, 100);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 100.0);
    assertLinux(bd);
  });

  it('testFirefox3', () => {
    let bd = new BrowserDetails(FIREFOX30_WINDOWS);
    assertGecko(bd);
    assertFirefox(bd);
    assertBrowserMajorVersion(bd, 3);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 1.9);
    assertWindows(bd);

    bd = new BrowserDetails(FIREFOX30_LINUX);
    assertGecko(bd);
    assertFirefox(bd);
    assertBrowserMajorVersion(bd, 3);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 1.9);
    assertLinux(bd);
  });

  it('testFirefox33Android', () => {
    const bd = new BrowserDetails(FIREFOX33_ANDROID);
    assertGecko(bd);
    assertFirefox(bd);
    assertBrowserMajorVersion(bd, 33);
    assertBrowserMinorVersion(bd, 0);
    assertAndroidVersion(bd, -1, -1);
  });

  it('testFirefox35', () => {
    const bd = new BrowserDetails(FIREFOX35_WINDOWS);
    assertGecko(bd);
    assertFirefox(bd);
    assertBrowserMajorVersion(bd, 3);
    assertBrowserMinorVersion(bd, 5);
    assertEngineVersion(bd, 1.9);
    assertWindows(bd);
  });

  it('testFirefox36', () => {
    const bd = new BrowserDetails(FIREFOX36_WINDOWS);
    assertGecko(bd);
    assertFirefox(bd);
    assertBrowserMajorVersion(bd, 3);
    assertBrowserMinorVersion(bd, 6);
    assertEngineVersion(bd, 1.9);
    assertWindows(bd);
  });

  it('testFirefox30b5', () => {
    const bd = new BrowserDetails(FIREFOX_30B5_MAC);
    assertGecko(bd);
    assertFirefox(bd);
    assertBrowserMajorVersion(bd, 3);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 1.9);
    assertMacOSX(bd);
  });

  it('testFirefox40b11', () => {
    const bd = new BrowserDetails(FIREFOX_40B11_WIN);
    assertGecko(bd);
    assertFirefox(bd);
    assertBrowserMajorVersion(bd, 4);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 2.0);
    assertWindows(bd);
  });

  it('testFirefox40b7', () => {
    const bd = new BrowserDetails(FIREFOX_40B7_WIN);
    assertGecko(bd);
    assertFirefox(bd);
    assertBrowserMajorVersion(bd, 4);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 2.0);
    assertWindows(bd);
  });

  it('testKonquerorLinux', () => {
    // Just ensure detection does not crash
    const bd = new BrowserDetails(KONQUEROR_LINUX);
    assertLinux(bd);
  });

  it('testFirefox36b', () => {
    const bd = new BrowserDetails(FIREFOX36B_MAC);
    assertGecko(bd);
    assertFirefox(bd);
    assertBrowserMajorVersion(bd, 3);
    assertBrowserMinorVersion(bd, 6);
    assertEngineVersion(bd, 1.9);
    assertMacOSX(bd);
  });

  it('testOpera964', () => {
    const bd = new BrowserDetails(OPERA964_WINDOWS);
    assertPresto(bd);
    assertOpera(bd);
    assertBrowserMajorVersion(bd, 9);
    assertBrowserMinorVersion(bd, 64);
    assertWindows(bd);
  });

  it('testOpera1010', () => {
    const bd = new BrowserDetails(OPERA1010_WINDOWS);
    assertPresto(bd);
    assertOpera(bd);
    assertBrowserMajorVersion(bd, 10);
    assertBrowserMinorVersion(bd, 10);
    assertWindows(bd);
  });

  it('testOpera1050', () => {
    const bd = new BrowserDetails(OPERA1050_WINDOWS);
    assertPresto(bd);
    assertOpera(bd);
    assertBrowserMajorVersion(bd, 10);
    assertBrowserMinorVersion(bd, 50);
    assertWindows(bd);
  });

  it('testIE6', () => {
    const bd = new BrowserDetails(IE6_WINDOWS);
    assertEngineVersion(bd, -1);
    assertIE(bd);
    assertBrowserMajorVersion(bd, 6);
    assertBrowserMinorVersion(bd, 0);
    assertWindows(bd);
  });

  it('testIE7', () => {
    const bd = new BrowserDetails(IE7_WINDOWS);
    assertEngineVersion(bd, -1);
    assertIE(bd);
    assertBrowserMajorVersion(bd, 7);
    assertBrowserMinorVersion(bd, 0);
    assertWindows(bd);
  });

  it('testIE8', () => {
    const bd = new BrowserDetails(IE8_WINDOWS);
    assertTrident(bd);
    assertEngineVersion(bd, 4);
    assertIE(bd);
    assertBrowserMajorVersion(bd, 8);
    assertBrowserMinorVersion(bd, 0);
    assertWindows(bd);
  });

  it('testIE9', () => {
    const bd = new BrowserDetails(IE9_BETA_WINDOWS_7);
    assertTrident(bd);
    assertEngineVersion(bd, 5);
    assertIE(bd);
    assertBrowserMajorVersion(bd, 9);
    assertBrowserMinorVersion(bd, 0);
    assertWindows(bd);
  });

  it('testIE9InIE7CompatibilityMode', () => {
    const bd = new BrowserDetails(IE9_IN_IE7_MODE_WINDOWS_7);
    assertTrident(bd);
    assertEngineVersion(bd, 5);
    assertIE(bd);
    assertBrowserMajorVersion(bd, 9);
    assertBrowserMinorVersion(bd, 0);
    assertWindows(bd);
  });

  it('testIE9InIE8CompatibilityMode', () => {
    const bd = new BrowserDetails(IE9_BETA_IN_IE8_MODE_WINDOWS_7);
    /*
     * Trident/4.0 in example user agent string based on beta even though it
     * should be Trident/5.0 in real (non-beta) user agent strings
     */
    assertTrident(bd);
    assertEngineVersion(bd, 4);
    assertIE(bd);
    assertBrowserMajorVersion(bd, 8);
    assertBrowserMinorVersion(bd, 0);
    assertWindows(bd);
  });

  it('testIE10', () => {
    const bd = new BrowserDetails(IE10_WINDOWS_8);
    assertTrident(bd);
    assertEngineVersion(bd, 6);
    assertIE(bd);
    assertBrowserMajorVersion(bd, 10);
    assertBrowserMinorVersion(bd, 0);
    assertWindows(bd);
  });

  it('testIE11', () => {
    const bd = new BrowserDetails(IE11_WINDOWS_7);
    assertTrident(bd);
    assertEngineVersion(bd, 7);
    assertIE(bd);
    assertBrowserMajorVersion(bd, 11);
    assertBrowserMinorVersion(bd, 0);
    assertWindows(bd);
  });

  it('testIE11Windows7CompatibilityViewIE7', () => {
    const bd = new BrowserDetails(IE11_IN_IE7_MODE_WINDOWS_7);
    assertTrident(bd);
    assertEngineVersion(bd, 7);
    assertIE(bd);
    assertBrowserMajorVersion(bd, 11);
    assertBrowserMinorVersion(bd, 0);
    assertWindows(bd);
  });

  it('testIE11Windows10CompatibilityViewIE7', () => {
    const bd = new BrowserDetails(IE11_IN_IE7_MODE_WINDOWS_10);
    assertTrident(bd);
    assertEngineVersion(bd, 7);
    assertIE(bd);
    assertBrowserMajorVersion(bd, 11);
    assertBrowserMinorVersion(bd, 0);
    assertWindows(bd);
  });

  it('testIE11LaunchDayWindows10CompatibilityViewIE7', () => {
    const bd = new BrowserDetails(IE11_IN_IE7_MODE_LAUNCH_DAY_WINDOWS_10);
    assertTrident(bd);
    /*
     * Trident/8.0 in example user agent string based on launch day even
     * though it should be Trident/7.0 in user agent strings for up-to-date
     * Windows 10 IE11
     */
    assertEngineVersion(bd, 7);
    assertIE(bd);
    assertBrowserMajorVersion(bd, 11);
    assertBrowserMinorVersion(bd, 0);
    assertWindows(bd);
  });

  it('testIE11WindowsPhone81Update', () => {
    const bd = new BrowserDetails(IE11_WINDOWS_PHONE_8_1_UPDATE);
    assertTrident(bd);
    assertEngineVersion(bd, 7);
    assertIE(bd);
    assertBrowserMajorVersion(bd, 11);
    assertBrowserMinorVersion(bd, 0);
    assertWindows(bd, true);
  });

  it('testEdgeWindows10', () => {
    const bd = new BrowserDetails(EDGE_12_WINDOWS_10);
    assertEdge(bd);
    assertBrowserMajorVersion(bd, 12);
    assertBrowserMinorVersion(bd, 10240);
    assertWindows(bd, false);
  });

  it('testEdgeWindows11', () => {
    const bd = new BrowserDetails(EDGE_100);
    assertEdge(bd);
    assertBrowserMajorVersion(bd, 100);
    assertBrowserMinorVersion(bd, 0);
    assertWindows(bd, false);
  });

  it('testEdgeMac', () => {
    const bd = new BrowserDetails(EDGE_99_MAC);
    assertEdge(bd);
    assertBrowserMajorVersion(bd, 99);
    assertBrowserMinorVersion(bd, 0);
    assertMacOSX(bd);
  });

  it('testEdgeAndroid', () => {
    const bd = new BrowserDetails(EDGE_97_ANDROID);
    assertEdge(bd);
    assertBrowserMajorVersion(bd, 97);
    assertBrowserMinorVersion(bd, 0);
    assertAndroidVersion(bd, 10, -1);
  });

  it('testEdgeIOS', () => {
    const bd = new BrowserDetails(EDGE_97_IOS);
    assertEdge(bd);
    assertBrowserMajorVersion(bd, 97);
    assertBrowserMinorVersion(bd, 1072);
    assertIPhone(bd);
  });

  it('testEclipseMac_safari91', () => {
    const bd = new BrowserDetails(ECLIPSE_MAC_SAFARI_91);
    assertWebKit(bd);
    assertSafari(bd);
    assertBrowserMajorVersion(bd, 9);
    assertBrowserMinorVersion(bd, 1);
    assertEngineVersion(bd, 601.5);
    assertMacOSX(bd);
  });

  it('testEclipseMac_safari90', () => {
    const bd = new BrowserDetails(ECLIPSE_MAC_SAFARI_90);
    assertWebKit(bd);
    assertSafari(bd);
    assertBrowserMajorVersion(bd, 9);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 601.4);
    assertMacOSX(bd);
  });

  it('testHeadlessChrome', () => {
    const userAgent =
      'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) HeadlessChrome/60.0.3112.101 Safari/537.36';
    const bd = new BrowserDetails(userAgent);
    assertWebKit(bd);
    assertChrome(bd);
    assertBrowserMajorVersion(bd, 60);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 537.36);
    assertLinux(bd);
  });

  it('testOpera65', () => {
    const bd = new BrowserDetails(OPERA115_WINDOWS);
    assertWebKit(bd);
    assertOpera(bd);
    assertBrowserMajorVersion(bd, 115);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 537.36);
    assertWindows(bd);
  });

  it('testIos11FacebookBrowser', () => {
    const bd = new BrowserDetails(IPHONE_IOS_11_FACEBOOK_BROWSER);
    assertWebKit(bd);
    assertEngineVersion(bd, 605.1);
  });

  it('testIos11Firefox', () => {
    const bd = new BrowserDetails(IPHONE_IOS_11_FIREFOX);
    assertWebKit(bd);
    assertEngineVersion(bd, 604.3);
  });

  it('testCommonDesktopUserAgents', () => {
    assertAgentDetails(COMMON_DESKTOP_USER_AGENTS);
  });

  it('testMobileUserAgents', () => {
    assertAgentDetails(MOBILE_USER_AGENTS);
  });

  it('testByteSpiderWebCrawler', () => {
    const bd = new BrowserDetails(BYTE_SPIDER);
    assertWebKit(bd);
    assertSafari(bd);
    assertBrowserMajorVersion(bd, -1);
    assertBrowserMinorVersion(bd, -1);
    assertEngineVersion(bd, 537.36);
    assertAndroidVersion(bd, 5, 0);
  });

  it('testDuckDuckBot1', () => {
    const bd = new BrowserDetails(DUCK_DUCK_BOT);
    assertUnspecifiedBrowser(bd);
    assertBrowserMajorVersion(bd, -1);
    assertBrowserMinorVersion(bd, -1);
    assertEngineVersion(bd, -1);
    assertAndroidVersion(bd, 5, 169);
  });

  it('testDuckDuckBot2', () => {
    const bd = new BrowserDetails(DUCK_DUCK_BOT_2);
    assertBrowserMajorVersion(bd, 130);
    assertBrowserMinorVersion(bd, 0);
    assertEngineVersion(bd, 537.36);
    assertAndroidVersion(bd, 14, -1);
  });

  it('testDuckDuckBot3', () => {
    let bd = new BrowserDetails(DUCK_DUCK_BOT_3);
    assertUnspecifiedBrowser(bd);
    assertBrowserMajorVersion(bd, -1);
    assertBrowserMinorVersion(bd, -1);
    assertEngineVersion(bd, -1);

    bd = new BrowserDetails('DuckDuckGo');
    assertUnspecifiedBrowser(bd);
    assertBrowserMajorVersion(bd, -1);
    assertBrowserMinorVersion(bd, -1);
    assertEngineVersion(bd, -1);

    bd = new BrowserDetails('DuckDuckGo/5');
    assertUnspecifiedBrowser(bd);
    assertBrowserMajorVersion(bd, -1);
    assertBrowserMinorVersion(bd, -1);
    assertEngineVersion(bd, -1);
  });

  // beyond the Java suite (PORTING.md 13.6)
  it('flags unsupported browsers via isTooOldToFunctionProperly', () => {
    expect(new BrowserDetails(IE8_WINDOWS).isTooOldToFunctionProperly()).to.be.true;
    expect(new BrowserDetails(CHROME100_WINDOWS).isTooOldToFunctionProperly()).to.be.false;
  });
});
