/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.shared;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import tools.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;

import com.vaadin.flow.server.frontend.TaskGenerateTsConfigTest;

public class BrowserDetailsTest extends TestCase {

    private static final String FIREFOX30_WINDOWS = "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.0.6) Gecko/2009011913 Firefox/3.0.6";
    private static final String FIREFOX30_LINUX = "Mozilla/5.0 (X11; U; Linux x86_64; es-ES; rv:1.9.0.12) Gecko/2009070811 Ubuntu/9.04 (jaunty) Firefox/3.0.12";
    private static final String FIREFOX33_ANDROID = "Mozilla/5.0 (Android; Tablet; rv:33.0) Gecko/33.0 Firefox/33.0";
    private static final String FIREFOX35_WINDOWS = "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.1.8) Gecko/20100202 Firefox/3.5.8 (.NET CLR 3.5.30729) FirePHP/0.4";
    private static final String FIREFOX36_WINDOWS = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2) Gecko/20100115 Firefox/3.6 (.NET CLR 3.5.30729)";
    private static final String FIREFOX36B_MAC = "UAString mozilla/5.0 (macintosh; u; intel mac os x 10.6; en-us; rv:1.9.2) gecko/20100115 firefox/3.6";
    private static final String FIREFOX_30B5_MAC = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9b5) Gecko/2008032619 Firefox/3.0b5";
    private static final String FIREFOX_40B7_WIN = "Mozilla/5.0 (Windows NT 5.1; rv:2.0b7) Gecko/20100101 Firefox/4.0b7";
    private static final String FIREFOX_40B11_WIN = "Mozilla/5.0 (Windows NT 5.1; rv:2.0b11) Gecko/20100101 Firefox/4.0b11";
    private static final String KONQUEROR_LINUX = "Mozilla/5.0 (compatible; Konqueror/3.5; Linux) KHTML/3.5.5 (like Gecko) (Exabot-Thumbnails)";

    private static final String IE6_WINDOWS = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 2.0.50727)";
    private static final String IE7_WINDOWS = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; .NET CLR 2.0.50727; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)";

    private static final String IE8_WINDOWS = "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729; InfoPath.2)";

    private static final String IE9_IN_IE7_MODE_WINDOWS_7 = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.1; Trident/5.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C)";
    private static final String IE9_BETA_IN_IE8_MODE_WINDOWS_7 = "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; Trident/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C)";
    private static final String IE9_BETA_WINDOWS_7 = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)";

    private static final String IE10_WINDOWS_8 = "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; WOW64; Trident/6.0)";
    private static final String IE11_WINDOWS_7 = "Mozilla/5.0 (Windows NT 6.1; Trident/7.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; rv:11.0) like Gecko";
    private static final String IE11_IN_IE7_MODE_WINDOWS_7 = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.1; WOW64; Trident/7.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E)";
    private static final String IE11_IN_IE7_MODE_WINDOWS_10 = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 10.0; WOW64; Trident/7.0; .NET4.0C; .NET4.0E)";
    private static final String IE11_IN_IE7_MODE_LAUNCH_DAY_WINDOWS_10 = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 10.0; WOW64; Trident/8.0; .NET4.0C; .NET4.0E)";
    private static final String IE11_WINDOWS_PHONE_8_1_UPDATE = "Mozilla/5.0 (Mobile; Windows Phone 8.1; Android 4.0; ARM; Trident/7.0; Touch; rv:11.0; IEMobile/11.0; NOKIA; Lumia 920) Like iPhone OS 7_0_3 Mac OS X AppleWebKit/537 (KHTML, like Gecko) Mobile Safari/537";

    // "Version/" was added in 10.00
    private static final String OPERA964_WINDOWS = "Opera/9.64(Windows NT 5.1; U; en) Presto/2.1.1";
    private static final String OPERA1010_WINDOWS = "Opera/9.80 (Windows NT 5.1; U; en) Presto/2.2.15 Version/10.10";
    private static final String OPERA1050_WINDOWS = "Opera/9.80 (Windows NT 5.1; U; en) Presto/2.5.22 Version/10.50";
    private static final String OPERA115_WINDOWS = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 OPR/115.0.0.0";

    private static final String CHROME3_MAC = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_5_8; en-US) AppleWebKit/532.0 (KHTML, like Gecko) Chrome/3.0.198 Safari/532.0";
    private static final String CHROME4_WINDOWS = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/532.5 (KHTML, like Gecko) Chrome/4.0.249.89 Safari/532.5";
    private static final String CHROME_IOS = "Mozilla/5.0 (iPhone; CPU iPhone OS 9_2_1 like Mac OS X) AppleWebKit/601.1 (KHTML, like Gecko) CriOS/49.0.2623.73 Mobile/13D15 Safari/601.1.46";
    private static final String CHROME_40_ON_CHROMEOS = "Mozilla/5.0 (X11; CrOS x86_64 6457.31.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.38 Safari/537.36";

    private static final String CHROME_IOS_DESKTOP = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_5) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/85 Version/11.1.1 Safari/605.1.15";

    private static final String SAFARI3_WINDOWS = "Mozilla/5.0 (Windows; U; Windows NT 5.1; cs-CZ) AppleWebKit/525.28.3 (KHTML, like Gecko) Version/3.2.3 Safari/525.29";
    private static final String SAFARI4_MAC = "Mozilla/5.0 (Macintosh; U; PPC Mac OS X 10_5_8; en-us) AppleWebKit/531.22.7 (KHTML, like Gecko) Version/4.0.5 Safari/531.22.7";
    private static final String SAFARI10_WINDOWS = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/603.3.8 (KHTML, like Gecko) Version/10.1.2 Safari/603.3.8";
    private static final String SAFARI11_MAC = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/11.1 Safari/605.1.15";

    private static final String IPHONE_IOS_5_1 = "Mozilla/5.0 (iPhone; CPU iPhone OS 5_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9B179 Safari/7534.48.3";
    private static final String IPHONE_IOS_4_0 = "Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_0 like Mac OS X; en-us) AppleWebKit/532.9 (KHTML, like Gecko) Version/4.0.5 Mobile/8A293 Safari/6531.22.7";
    private static final String IPAD_IOS_4_3_1 = "Mozilla/5.0 (iPad; U; CPU OS 4_3_1 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8G4 Safari/6533.18.5";

    // application on the home screen, without Safari in user agent
    private static final String IPHONE_IOS_6_1_HOMESCREEN_SIMULATOR = "Mozilla/5.0 (iPhone; CPU iPhone OS 6_1 like Mac OS X) AppleWebKit/536.26 (KHTML, like Gecko) Mobile/10B141";

    private static final String ANDROID_HTC_2_1 = "Mozilla/5.0 (Linux; U; Android 2.1-update1; en-us; ADR6300 Build/ERE27) AppleWebKit/530.17 (KHTML, like Gecko) Version/4.0 Mobile Safari/530.17";
    private static final String ANDROID_GOOGLE_NEXUS_2_2 = "Mozilla/5.0 (Linux; U; Android 2.2; en-us; Nexus One Build/FRF91) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";
    private static final String ANDROID_MOTOROLA_3_0 = "Mozilla/5.0 (Linux; U; Android 3.0; en-us; Xoom Build/HRI39) AppleWebKit/534.13 (KHTML, like Gecko) Version/4.0 Safari/534.13";
    private static final String ANDROID_GALAXY_NEXUS_4_0_4_CHROME = "Mozilla/5.0 (Linux; Android 4.0.4; Galaxy Nexus Build/IMM76B) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.133 Mobile Safari/535.19";
    private static final String ANDROID_CALLPOD_KEEPER = "callpod keeper for android 1.0 (10.1.1/240) dalvik/2.1.0 (linux; u; android 6.0; lg-v495 build/mra58k)";

    private static final String EDGE_12_WINDOWS_10 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/12.10240";

    private static final String ECLIPSE_MAC_SAFARI_91 = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_4) AppleWebKit/601.5.17 (KHTML, like Gecko) Safari/522.0";
    private static final String ECLIPSE_MAC_SAFARI_90 = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_4) AppleWebKit/601.4.4 (KHTML, like Gecko) Safari/522.0";

    private static final String IPHONE_IOS_11_FACEBOOK_BROWSER = "Mozilla/5.0 (iPhone; CPU iPhone OS 11_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E302 [FBAN/MessengerForiOS;FBAV/165.0.0.45.95;FBBV/107115338;FBDV/iPhone10,6;FBMD/iPhone;FBSN/iOS;FBSV/11.3.1;FBSS/3;FBCR/DNA;FBID/phone;FBLC/en_GB;FBOP/5;FBRV/0]";
    private static final String IPHONE_IOS_11_FIREFOX = "Mozilla/5.0 (iPhone; CPU iPhone OS 11_1_2 like Mac OS X) AppleWebKit/604.3.5 (KHTML, like Gecko) FxiOS/11.1b10377 Mobile/15B202 Safari/604.3.5";

    private static final String EDGE_100 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.60 Safari/537.36 Edg/100.0.1185.29";

    private static final String EDGE_99_MAC = "Mozilla/5.0 (Macintosh; Intel Mac OS X 12_3_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.75 Safari/537.36 Edg/99.0.1150.36";
    private static final String EDGE_97_ANDROID = "Mozilla/5.0 (Linux; Android 10; Pixel 3 XL) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.79 Mobile Safari/537.36 EdgA/97.0.1072.69";
    private static final String EDGE_97_IOS = "Mozilla/5.0 (iPhone; CPU iPhone OS 15_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 EdgiOS/97.1072.69 Mobile/15E148 Safari/605.1.15";

    // Version 100 Strings
    private static final String CHROME100_WINDOWS = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4844.84 Safari/537.36";
    private static final String FIREFOX_100_WIN64 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:100.0) Gecko/20100101 Firefox/100.0";
    private static final String FIREFOX_100_WIN32 = "Mozilla/5.0 (Windows NT 10.0; rv:100.0) Gecko/20100101 Firefox/100.0";
    private static final String FIREFOX_100_MACOS = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:100.0) Gecko/20100101 Firefox/100.0";
    private static final String FIREFOX_100_LINUX = "Mozilla/5.0 (X11; Linux x86_64; rv:100.0) Gecko/20100101 Firefox/100.0";

    // Web crawlers and bots
    private static final String BYTE_SPIDER = "mozilla/5.0 (linux; android 5.0) applewebkit/537.36 (khtml, like gecko) mobile safari/537.36 (compatible; bytespider; spider-feedback@bytedance.com)";
    private static final String DUCK_DUCK_BOT = "ddg_android/5.169.0 (com.duckduckgo.mobile.android; android api 33)";
    private static final String DUCK_DUCK_BOT_2 = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/130.0.6723.106 Mobile DuckDuckGo/5 Safari/537.36";
    private static final String DUCK_DUCK_BOT_3 = "DuckDuckGo/0.26.3 CFNetwork/1331.0.7 Darwin/21.4.0";

    public void testSafari3() {
        BrowserDetails bd = new BrowserDetails(SAFARI3_WINDOWS);
        assertWebKit(bd);
        assertSafari(bd);
        assertBrowserMajorVersion(bd, 3);
        assertBrowserMinorVersion(bd, 2);
        assertEngineVersion(bd, 525.28f);
        assertWindows(bd);
    }

    public void testSafari4() {
        BrowserDetails bd = new BrowserDetails(SAFARI4_MAC);
        assertWebKit(bd);
        assertSafari(bd);
        assertBrowserMajorVersion(bd, 4);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 531.22f);
        assertMacOSX(bd);
    }

    public void testSafari10() {
        BrowserDetails bd = new BrowserDetails(SAFARI10_WINDOWS);
        assertWebKit(bd);
        assertSafari(bd);
        assertBrowserMajorVersion(bd, 10);
        assertBrowserMinorVersion(bd, 1);
        assertEngineVersion(bd, 603.3f);
        assertMacOSX(bd);
    }

    public void testSafari11() {
        BrowserDetails bd = new BrowserDetails(SAFARI11_MAC);
        assertWebKit(bd);
        assertSafari(bd);
        assertBrowserMajorVersion(bd, 11);
        assertBrowserMinorVersion(bd, 1);
        assertEngineVersion(bd, 605.1f);
        assertMacOSX(bd);
    }

    public void testIPhoneIOS6Homescreen() {
        BrowserDetails bd = new BrowserDetails(
                IPHONE_IOS_6_1_HOMESCREEN_SIMULATOR);
        assertWebKit(bd);
        // not identified as Safari, no browser version available
        assertEngineVersion(bd, 536.26f);
        assertIPhone(bd);

    }

    public void testIPhoneIOS5() {
        BrowserDetails bd = new BrowserDetails(IPHONE_IOS_5_1);
        assertWebKit(bd);
        assertSafari(bd);
        assertBrowserMajorVersion(bd, 5);
        assertBrowserMinorVersion(bd, 1);
        assertEngineVersion(bd, 534.46f);
        assertIPhone(bd);

    }

    public void testIPhoneIOS4() {
        BrowserDetails bd = new BrowserDetails(IPHONE_IOS_4_0);
        assertWebKit(bd);
        assertSafari(bd);
        assertBrowserMajorVersion(bd, 4);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 532.9f);
        assertIPhone(bd);
    }

    public void testIPadIOS4() {
        BrowserDetails bd = new BrowserDetails(IPAD_IOS_4_3_1);
        assertWebKit(bd);
        assertSafari(bd);
        assertBrowserMajorVersion(bd, 5);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 533.17f);
    }

    public void testAndroid21() {
        BrowserDetails bd = new BrowserDetails(ANDROID_HTC_2_1);
        assertWebKit(bd);
        assertSafari(bd);
        assertBrowserMajorVersion(bd, 4);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 530.17f);
        assertAndroid(bd, 2, 1);

    }

    public void testAndroid22() {
        BrowserDetails bd = new BrowserDetails(ANDROID_GOOGLE_NEXUS_2_2);
        assertWebKit(bd);
        assertSafari(bd);
        assertBrowserMajorVersion(bd, 4);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 533.1f);
        assertAndroid(bd, 2, 2);

    }

    public void testAndroid30() {
        BrowserDetails bd = new BrowserDetails(ANDROID_MOTOROLA_3_0);
        assertWebKit(bd);
        assertSafari(bd);
        assertBrowserMajorVersion(bd, 4);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 534.13f);
        assertAndroid(bd, 3, 0);

    }

    public void testAndroid40Chrome() {
        BrowserDetails bd = new BrowserDetails(
                ANDROID_GALAXY_NEXUS_4_0_4_CHROME);
        assertWebKit(bd);
        assertChrome(bd);
        assertBrowserMajorVersion(bd, 18);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 535.19f);
        assertAndroid(bd, 4, 0);

    }

    public void testAndroidCallpodKeeper() {
        BrowserDetails bd = new BrowserDetails(ANDROID_CALLPOD_KEEPER);
        assertOSMajorVersion(bd, 6);
        assertOSMinorVersion(bd, 0);
        assertEngineVersion(bd, -1);

    }

    private void assertOSMajorVersion(BrowserDetails bd, int i) {
        assertEquals(i, bd.getOperatingSystemMajorVersion());
    }

    private void assertOSMinorVersion(BrowserDetails bd, int i) {
        assertEquals(i, bd.getOperatingSystemMinorVersion());
    }

    public void testChrome3() {
        BrowserDetails bd = new BrowserDetails(CHROME3_MAC);
        assertWebKit(bd);
        assertChrome(bd);
        assertBrowserMajorVersion(bd, 3);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 532.0f);
        assertMacOSX(bd);

    }

    public void testChrome4() {
        BrowserDetails bd = new BrowserDetails(CHROME4_WINDOWS);
        assertWebKit(bd);
        assertChrome(bd);
        assertBrowserMajorVersion(bd, 4);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 532.5f);
        assertWindows(bd);

    }

    public void testChromeIOS() {
        BrowserDetails bd = new BrowserDetails(CHROME_IOS);
        assertWebKit(bd);
        assertChrome(bd);
        assertBrowserMajorVersion(bd, 49);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 601.1f);
    }

    public void testChromeIOSDesktopSiteFeature() {
        BrowserDetails bd = new BrowserDetails(CHROME_IOS_DESKTOP);
        assertWebKit(bd);
        assertChrome(bd);
        assertBrowserMajorVersion(bd, 85);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 605.1f);
    }

    public void testChromeChromeOS() {
        BrowserDetails bd = new BrowserDetails(CHROME_40_ON_CHROMEOS);
        assertWebKit(bd);
        assertChrome(bd);
        assertBrowserMajorVersion(bd, 40);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 537.36f);
        assertChromeOS(bd, 6457, 31);

    }

    public void testChrome100Windows() {
        BrowserDetails bd = new BrowserDetails(CHROME100_WINDOWS);
        assertWebKit(bd);
        assertChrome(bd);
        assertBrowserMajorVersion(bd, 100);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 537.36f);
        assertWindows(bd);
    }

    public void testFirefox100Windows() {
        BrowserDetails bd = new BrowserDetails(FIREFOX_100_WIN64);
        assertGecko(bd);
        assertFirefox(bd);
        assertBrowserMajorVersion(bd, 100);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 100.0f);
        assertWindows(bd);
    }

    public void testFirefox100Windows32() {
        BrowserDetails bd = new BrowserDetails(FIREFOX_100_WIN32);
        assertGecko(bd);
        assertFirefox(bd);
        assertBrowserMajorVersion(bd, 100);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 100.0f);
        assertWindows(bd);
    }

    public void testFirefox100MacOs() {
        BrowserDetails bd = new BrowserDetails(FIREFOX_100_MACOS);
        assertGecko(bd);
        assertFirefox(bd);
        assertBrowserMajorVersion(bd, 100);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 100.0f);
        assertMacOSX(bd);
    }

    public void testFirefox100Linux() {
        BrowserDetails bd = new BrowserDetails(FIREFOX_100_LINUX);
        assertGecko(bd);
        assertFirefox(bd);
        assertBrowserMajorVersion(bd, 100);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 100.0f);
        assertLinux(bd);
    }

    public void testFirefox3() {
        BrowserDetails bd = new BrowserDetails(FIREFOX30_WINDOWS);
        assertGecko(bd);
        assertFirefox(bd);
        assertBrowserMajorVersion(bd, 3);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 1.9f);
        assertWindows(bd);

        bd = new BrowserDetails(FIREFOX30_LINUX);
        assertGecko(bd);
        assertFirefox(bd);
        assertBrowserMajorVersion(bd, 3);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 1.9f);
        assertLinux(bd);

    }

    public void testFirefox33Android() {
        BrowserDetails bd = new BrowserDetails(FIREFOX33_ANDROID);
        assertGecko(bd);
        assertFirefox(bd);
        assertBrowserMajorVersion(bd, 33);
        assertBrowserMinorVersion(bd, 0);
        assertAndroid(bd, -1, -1);

    }

    public void testFirefox35() {
        BrowserDetails bd = new BrowserDetails(FIREFOX35_WINDOWS);
        assertGecko(bd);
        assertFirefox(bd);
        assertBrowserMajorVersion(bd, 3);
        assertBrowserMinorVersion(bd, 5);
        assertEngineVersion(bd, 1.9f);
        assertWindows(bd);

    }

    public void testFirefox36() {
        BrowserDetails bd = new BrowserDetails(FIREFOX36_WINDOWS);
        assertGecko(bd);
        assertFirefox(bd);
        assertBrowserMajorVersion(bd, 3);
        assertBrowserMinorVersion(bd, 6);
        assertEngineVersion(bd, 1.9f);
        assertWindows(bd);

    }

    public void testFirefox30b5() {
        BrowserDetails bd = new BrowserDetails(FIREFOX_30B5_MAC);
        assertGecko(bd);
        assertFirefox(bd);
        assertBrowserMajorVersion(bd, 3);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 1.9f);
        assertMacOSX(bd);

    }

    public void testFirefox40b11() {
        BrowserDetails bd = new BrowserDetails(FIREFOX_40B11_WIN);
        assertGecko(bd);
        assertFirefox(bd);
        assertBrowserMajorVersion(bd, 4);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 2.0f);
        assertWindows(bd);

    }

    public void testFirefox40b7() {
        BrowserDetails bd = new BrowserDetails(FIREFOX_40B7_WIN);
        assertGecko(bd);
        assertFirefox(bd);
        assertBrowserMajorVersion(bd, 4);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 2.0f);
        assertWindows(bd);

    }

    public void testKonquerorLinux() {
        // Just ensure detection does not crash
        BrowserDetails bd = new BrowserDetails(KONQUEROR_LINUX);
        assertLinux(bd);
    }

    public void testFirefox36b() {
        BrowserDetails bd = new BrowserDetails(FIREFOX36B_MAC);
        assertGecko(bd);
        assertFirefox(bd);
        assertBrowserMajorVersion(bd, 3);
        assertBrowserMinorVersion(bd, 6);
        assertEngineVersion(bd, 1.9f);
        assertMacOSX(bd);

    }

    public void testOpera964() {
        BrowserDetails bd = new BrowserDetails(OPERA964_WINDOWS);
        assertPresto(bd);
        assertOpera(bd);
        assertBrowserMajorVersion(bd, 9);
        assertBrowserMinorVersion(bd, 64);
        assertWindows(bd);

    }

    public void testOpera1010() {
        BrowserDetails bd = new BrowserDetails(OPERA1010_WINDOWS);
        assertPresto(bd);
        assertOpera(bd);
        assertBrowserMajorVersion(bd, 10);
        assertBrowserMinorVersion(bd, 10);
        assertWindows(bd);

    }

    public void testOpera1050() {
        BrowserDetails bd = new BrowserDetails(OPERA1050_WINDOWS);
        assertPresto(bd);
        assertOpera(bd);
        assertBrowserMajorVersion(bd, 10);
        assertBrowserMinorVersion(bd, 50);
        assertWindows(bd);

    }

    public void testIE6() {
        BrowserDetails bd = new BrowserDetails(IE6_WINDOWS);
        assertEngineVersion(bd, -1);
        assertIE(bd);
        assertBrowserMajorVersion(bd, 6);
        assertBrowserMinorVersion(bd, 0);
        assertWindows(bd);

    }

    public void testIE7() {
        BrowserDetails bd = new BrowserDetails(IE7_WINDOWS);
        assertEngineVersion(bd, -1);
        assertIE(bd);
        assertBrowserMajorVersion(bd, 7);
        assertBrowserMinorVersion(bd, 0);
        assertWindows(bd);

    }

    public void testIE8() {
        BrowserDetails bd = new BrowserDetails(IE8_WINDOWS);
        assertTrident(bd);
        assertEngineVersion(bd, 4);
        assertIE(bd);
        assertBrowserMajorVersion(bd, 8);
        assertBrowserMinorVersion(bd, 0);
        assertWindows(bd);

    }

    public void testIE9() {
        BrowserDetails bd = new BrowserDetails(IE9_BETA_WINDOWS_7);
        assertTrident(bd);
        assertEngineVersion(bd, 5);
        assertIE(bd);
        assertBrowserMajorVersion(bd, 9);
        assertBrowserMinorVersion(bd, 0);
        assertWindows(bd);

    }

    public void testIE9InIE7CompatibilityMode() {
        BrowserDetails bd = new BrowserDetails(IE9_IN_IE7_MODE_WINDOWS_7);

        assertTrident(bd);
        assertEngineVersion(bd, 5);
        assertIE(bd);
        assertBrowserMajorVersion(bd, 9);
        assertBrowserMinorVersion(bd, 0);

        assertWindows(bd);

    }

    public void testIE9InIE8CompatibilityMode() {
        BrowserDetails bd = new BrowserDetails(IE9_BETA_IN_IE8_MODE_WINDOWS_7);

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

    }

    public void testIE10() {
        BrowserDetails bd = new BrowserDetails(IE10_WINDOWS_8);
        assertTrident(bd);
        assertEngineVersion(bd, 6);
        assertIE(bd);
        assertBrowserMajorVersion(bd, 10);
        assertBrowserMinorVersion(bd, 0);
        assertWindows(bd);

    }

    public void testIE11() {
        BrowserDetails bd = new BrowserDetails(IE11_WINDOWS_7);
        assertTrident(bd);
        assertEngineVersion(bd, 7);
        assertIE(bd);
        assertBrowserMajorVersion(bd, 11);
        assertBrowserMinorVersion(bd, 0);
        assertWindows(bd);

    }

    public void testIE11Windows7CompatibilityViewIE7() {
        BrowserDetails bd = new BrowserDetails(IE11_IN_IE7_MODE_WINDOWS_7);
        assertTrident(bd);
        assertEngineVersion(bd, 7);
        assertIE(bd);
        assertBrowserMajorVersion(bd, 11);
        assertBrowserMinorVersion(bd, 0);
        assertWindows(bd);

    }

    public void testIE11Windows10CompatibilityViewIE7() {
        BrowserDetails bd = new BrowserDetails(IE11_IN_IE7_MODE_WINDOWS_10);
        assertTrident(bd);
        assertEngineVersion(bd, 7);
        assertIE(bd);
        assertBrowserMajorVersion(bd, 11);
        assertBrowserMinorVersion(bd, 0);
        assertWindows(bd);

    }

    public void testIE11LaunchDayWindows10CompatibilityViewIE7() {
        BrowserDetails bd = new BrowserDetails(
                IE11_IN_IE7_MODE_LAUNCH_DAY_WINDOWS_10);
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

    }

    public void testIE11WindowsPhone81Update() {
        BrowserDetails bd = new BrowserDetails(IE11_WINDOWS_PHONE_8_1_UPDATE);
        assertTrident(bd);
        assertEngineVersion(bd, 7);
        assertIE(bd);
        assertBrowserMajorVersion(bd, 11);
        assertBrowserMinorVersion(bd, 0);
        assertWindows(bd, true);

    }

    public void testEdgeWindows10() {
        BrowserDetails bd = new BrowserDetails(EDGE_12_WINDOWS_10);
        assertEdge(bd);
        assertBrowserMajorVersion(bd, 12);
        assertBrowserMinorVersion(bd, 10240);
        assertWindows(bd, false);
    }

    public void testEdgeWindows11() {
        BrowserDetails bd = new BrowserDetails(EDGE_100);
        assertEdge(bd);
        assertBrowserMajorVersion(bd, 100);
        assertBrowserMinorVersion(bd, 0);
        assertWindows(bd, false);
    }

    public void testEdgeMac() {
        BrowserDetails bd = new BrowserDetails(EDGE_99_MAC);
        assertEdge(bd);
        assertBrowserMajorVersion(bd, 99);
        assertBrowserMinorVersion(bd, 0);
        assertMacOSX(bd);
    }

    public void testEdgeAndroid() {
        BrowserDetails bd = new BrowserDetails(EDGE_97_ANDROID);
        assertEdge(bd);
        assertBrowserMajorVersion(bd, 97);
        assertBrowserMinorVersion(bd, 0);
        assertAndroid(bd, 10, -1);
    }

    public void testEdgeIOS() {
        BrowserDetails bd = new BrowserDetails(EDGE_97_IOS);
        assertEdge(bd);
        assertBrowserMajorVersion(bd, 97);
        assertBrowserMinorVersion(bd, 1072);
        assertIPhone(bd);
    }

    public void testEclipseMac_safari91() {
        BrowserDetails bd = new BrowserDetails(ECLIPSE_MAC_SAFARI_91);
        assertWebKit(bd);
        assertSafari(bd);
        assertBrowserMajorVersion(bd, 9);
        assertBrowserMinorVersion(bd, 1);
        assertEngineVersion(bd, 601.5f);
        assertMacOSX(bd);

    }

    public void testEclipseMac_safari90() {
        BrowserDetails bd = new BrowserDetails(ECLIPSE_MAC_SAFARI_90);
        assertWebKit(bd);
        assertSafari(bd);
        assertBrowserMajorVersion(bd, 9);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 601.4f);
        assertMacOSX(bd);

    }

    public void testHeadlessChrome() {
        String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) HeadlessChrome/60.0.3112.101 Safari/537.36";
        BrowserDetails bd = new BrowserDetails(userAgent);
        assertWebKit(bd);
        assertChrome(bd);
        assertBrowserMajorVersion(bd, 60);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 537.36f);
        assertLinux(bd);
    }

    public void testOpera65() {
        String userAgent = OPERA115_WINDOWS;
        BrowserDetails bd = new BrowserDetails(userAgent);
        assertWebKit(bd);
        assertOpera(bd);
        assertBrowserMajorVersion(bd, 115);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 537.36f);
        assertWindows(bd);
    }

    public void testIos11FacebookBrowser() {
        BrowserDetails bd = new BrowserDetails(IPHONE_IOS_11_FACEBOOK_BROWSER);
        assertWebKit(bd);
        assertEngineVersion(bd, 605.1f);
    }

    public void testIos11Firefox() {
        BrowserDetails bd = new BrowserDetails(IPHONE_IOS_11_FIREFOX);
        assertWebKit(bd);
        assertEngineVersion(bd, 604.3f);
    }

    public void testCommonDesktopUserAgents() throws IOException {
        UserAgent[] agents = getUserAgentDetails(
                "common-desktop-useragents.json");

        assertAgentDetails(agents);
    }

    public void testMobileUserAgents() throws IOException {
        UserAgent[] agents = getUserAgentDetails("mobile-useragents.json");

        assertAgentDetails(agents);
    }

    public void testByteSpiderWebCrawler() {
        BrowserDetails bd = new BrowserDetails(BYTE_SPIDER);
        assertWebKit(bd);
        assertSafari(bd);
        assertBrowserMajorVersion(bd, -1);
        assertBrowserMinorVersion(bd, -1);
        assertEngineVersion(bd, 537.36f);
        assertAndroid(bd, 5, 0);
    }

    public void testDuckDuckBot1() {
        BrowserDetails bd = new BrowserDetails(DUCK_DUCK_BOT);
        assertUnspecifiedBrowser(bd);
        assertBrowserMajorVersion(bd, -1);
        assertBrowserMinorVersion(bd, -1);
        assertEngineVersion(bd, -1);
        assertAndroid(bd, 5, 169);
    }

    public void testDuckDuckBot2() {
        BrowserDetails bd = new BrowserDetails(DUCK_DUCK_BOT_2);
        assertBrowserMajorVersion(bd, 130);
        assertBrowserMinorVersion(bd, 0);
        assertEngineVersion(bd, 537.36f);
        assertAndroid(bd, 14, -1);
    }

    public void testDuckDuckBot3() {
        BrowserDetails bd = new BrowserDetails(DUCK_DUCK_BOT_3);
        assertUnspecifiedBrowser(bd);
        assertBrowserMajorVersion(bd, -1);
        assertBrowserMinorVersion(bd, -1);
        assertEngineVersion(bd, -1);

        bd = new BrowserDetails("DuckDuckGo");
        assertUnspecifiedBrowser(bd);
        assertBrowserMajorVersion(bd, -1);
        assertBrowserMinorVersion(bd, -1);
        assertEngineVersion(bd, -1);

        bd = new BrowserDetails("DuckDuckGo/5");
        assertUnspecifiedBrowser(bd);
        assertBrowserMajorVersion(bd, -1);
        assertBrowserMinorVersion(bd, -1);
        assertEngineVersion(bd, -1);
    }

    private static UserAgent[] getUserAgentDetails(String agentFile)
            throws IOException {
        String userAgents = IOUtils.toString(
                Objects.requireNonNull(TaskGenerateTsConfigTest.class
                        .getClassLoader().getResourceAsStream(agentFile)),
                StandardCharsets.UTF_8);
        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(userAgents, UserAgent[].class);
    }

    private void assertAgentDetails(UserAgent[] agents) {
        for (UserAgent agent : agents) {
            BrowserDetails bd = new BrowserDetails(agent.ua);
            assertOs(bd, agent.os);
            BrowserVersion versions = getMinorMajorVersion(
                    agent.browserVersion);
            Assert.assertEquals(
                    "Major version differs on userAgent " + agent.ua,
                    versions.browserMajorVersion, bd.getBrowserMajorVersion());
            Assert.assertEquals(
                    "Minor version differs on userAgent " + agent.ua,
                    versions.browserMinorVersion, bd.getBrowserMinorVersion());
        }
    }

    private BrowserVersion getMinorMajorVersion(String browserVersion) {
        final String[] digits = browserVersion.split("[-.]", 4);

        int major = Integer.parseInt(digits[0]);
        int minor = -1;
        if (digits.length >= 2) {
            minor = Integer.parseInt(digits[1]);
        }
        return new BrowserVersion(major, minor);
    }

    private void assertOs(BrowserDetails bd, String os) {
        switch (os) {
        case "LINUX":
            assertLinux(bd);
            break;
        case "WINDOWS":
            assertWindows(bd);
            break;
        case "MACOSX":
            assertMacOSX(bd);
            break;
        case "IPAD":
            assertIPad(bd);
            break;
        case "IPHONE":
            assertIPhone(bd);
            break;
        case "ANDROID":
            assertAndroid(bd);
            break;
        default:
            throw new AssertionError(os + " is not a supported OS");
        }
    }

    private record BrowserVersion(int browserMajorVersion,
            int browserMinorVersion) {
    }

    private record UserAgent(String ua, String browser, String browserVersion,
            String os, String device) {
    }

    /*
     * Helper methods below
     */

    private void assertEngineVersion(BrowserDetails browserDetails,
            float version) {
        assertEquals(version, browserDetails.getBrowserEngineVersion());

    }

    private void assertBrowserMajorVersion(BrowserDetails browserDetails,
            int version) {
        assertEquals(version, browserDetails.getBrowserMajorVersion());

    }

    private void assertBrowserMinorVersion(BrowserDetails browserDetails,
            int version) {
        assertEquals(version, browserDetails.getBrowserMinorVersion());

    }

    private void assertGecko(BrowserDetails browserDetails) {
        // Engine
        assertTrue(browserDetails.isGecko());
        assertFalse(browserDetails.isWebKit());
        assertFalse(browserDetails.isPresto());
        assertFalse(browserDetails.isTrident());
    }

    private void assertPresto(BrowserDetails browserDetails) {
        // Engine
        assertFalse(browserDetails.isGecko());
        assertFalse(browserDetails.isWebKit());
        assertTrue(browserDetails.isPresto());
        assertFalse(browserDetails.isTrident());
    }

    private void assertTrident(BrowserDetails browserDetails) {
        // Engine
        assertFalse(browserDetails.isGecko());
        assertFalse(browserDetails.isWebKit());
        assertFalse(browserDetails.isPresto());
        assertTrue(browserDetails.isTrident());
    }

    private void assertWebKit(BrowserDetails browserDetails) {
        // Engine
        assertFalse(browserDetails.isGecko());
        assertTrue(browserDetails.isWebKit());
        assertFalse(browserDetails.isPresto());
        assertFalse(browserDetails.isTrident());
    }

    private void assertFirefox(BrowserDetails browserDetails) {
        // Browser
        assertTrue(browserDetails.isFirefox());
        assertFalse(browserDetails.isChrome());
        assertFalse(browserDetails.isIE());
        assertFalse(browserDetails.isOpera());
        assertFalse(browserDetails.isSafari());
        assertFalse(browserDetails.isEdge());
    }

    private void assertChrome(BrowserDetails browserDetails) {
        // Browser
        assertFalse(browserDetails.isFirefox());
        assertTrue(browserDetails.isChrome());
        assertFalse(browserDetails.isIE());
        assertFalse(browserDetails.isOpera());
        assertFalse(browserDetails.isSafari());
        assertFalse(browserDetails.isEdge());
    }

    private void assertIE(BrowserDetails browserDetails) {
        // Browser
        assertFalse(browserDetails.isFirefox());
        assertFalse(browserDetails.isChrome());
        assertTrue(browserDetails.isIE());
        assertFalse(browserDetails.isOpera());
        assertFalse(browserDetails.isSafari());
        assertFalse(browserDetails.isEdge());
    }

    private void assertOpera(BrowserDetails browserDetails) {
        // Browser
        assertFalse(browserDetails.isFirefox());
        assertFalse(browserDetails.isChrome());
        assertFalse(browserDetails.isIE());
        assertTrue(browserDetails.isOpera());
        assertFalse(browserDetails.isSafari());
        assertFalse(browserDetails.isEdge());
    }

    private void assertSafari(BrowserDetails browserDetails) {
        // Browser
        assertFalse(browserDetails.isFirefox());
        assertFalse(browserDetails.isChrome());
        assertFalse(browserDetails.isIE());
        assertFalse(browserDetails.isOpera());
        assertTrue(browserDetails.isSafari());
        assertFalse(browserDetails.isEdge());
    }

    private void assertEdge(BrowserDetails browserDetails) {
        // Browser
        assertFalse(browserDetails.isFirefox());
        assertFalse(browserDetails.isChrome());
        assertFalse(browserDetails.isIE());
        assertFalse(browserDetails.isOpera());
        assertFalse(browserDetails.isSafari());
        assertTrue(browserDetails.isEdge());
    }

    private void assertUnspecifiedBrowser(BrowserDetails browserDetails) {
        assertFalse(browserDetails.isFirefox());
        assertFalse(browserDetails.isChrome());
        assertFalse(browserDetails.isIE());
        assertFalse(browserDetails.isOpera());
        assertFalse(browserDetails.isSafari());
        assertFalse(browserDetails.isEdge());
    }

    private void assertMacOSX(BrowserDetails browserDetails) {
        assertFalse(browserDetails.isLinux());
        assertFalse(browserDetails.isWindows());
        assertTrue(browserDetails.isMacOSX());
        assertFalse(browserDetails.isAndroid());
        assertFalse(browserDetails.isChromeOS());
    }

    private void assertAndroid(BrowserDetails browserDetails) {
        assertFalse(browserDetails.isLinux());
        assertFalse(browserDetails.isWindows());
        assertFalse(browserDetails.isMacOSX());
        assertTrue(browserDetails.isAndroid());
        assertFalse(browserDetails.isChromeOS());
    }

    private void assertAndroid(BrowserDetails browserDetails, int majorVersion,
            int minorVersion) {
        assertAndroid(browserDetails);

        assertOSMajorVersion(browserDetails, majorVersion);
        assertOSMinorVersion(browserDetails, minorVersion);
    }

    private void assertIPhone(BrowserDetails browserDetails) {
        assertTrue(browserDetails.isIPhone());
    }

    private void assertIPad(BrowserDetails browserDetails) {
        assertTrue(browserDetails.isIPad());
    }

    private void assertWindows(BrowserDetails browserDetails) {
        assertWindows(browserDetails, false);
    }

    private void assertWindows(BrowserDetails browserDetails,
            boolean isWindowsPhone) {
        assertFalse(browserDetails.isLinux());
        assertTrue(browserDetails.isWindows());
        assertFalse(browserDetails.isMacOSX());
        assertFalse(browserDetails.isAndroid());
        assertFalse(browserDetails.isChromeOS());
        Assert.assertEquals(isWindowsPhone, browserDetails.isWindowsPhone());
    }

    private void assertLinux(BrowserDetails browserDetails) {
        assertTrue(browserDetails.isLinux());
        assertFalse(browserDetails.isWindows());
        assertFalse(browserDetails.isMacOSX());
        assertFalse(browserDetails.isAndroid());
        assertFalse(browserDetails.isChromeOS());
    }

    private void assertChromeOS(BrowserDetails browserDetails, int majorVersion,
            int minorVersion) {
        assertFalse(browserDetails.isLinux());
        assertFalse(browserDetails.isWindows());
        assertFalse(browserDetails.isMacOSX());
        assertFalse(browserDetails.isAndroid());
        assertTrue(browserDetails.isChromeOS());

        assertOSMajorVersion(browserDetails, majorVersion);
        assertOSMinorVersion(browserDetails, minorVersion);
    }

}
