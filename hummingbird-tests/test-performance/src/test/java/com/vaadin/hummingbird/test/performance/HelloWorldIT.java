package com.vaadin.hummingbird.test.performance;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedCondition;

import com.vaadin.hummingbird.testutil.AbstractTestBenchTest;

import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.proxy.CaptureType;

/*
 * Copyright 2000-2016 Vaadin Ltd.
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

public class HelloWorldIT extends AbstractTestBenchTest {

    private BrowserMobProxyServer proxyServer;

    @Override
    protected String getTestPath() {
        return HelloWorldUI.PATH;
    }

    @Test
    public void timeUntilButtonPresent_5ms_1500KBs() throws Exception {
        // Intranet
        timeUntilButtonPresent(5, 1500);
    }

    @Test
    public void timeUntilButtonPresent_5ms_150KBs() throws Exception {
        // Low bandwidth
        timeUntilButtonPresent(5, 150);
    }

    @Test
    public void timeUntilButtonPresent_500ms_1500KBs() throws Exception {
        // High latency
        timeUntilButtonPresent(500, 1500);
    }

    public void timeUntilButtonPresent(int latencyMilliseconds,
            int bandwidthKilobytePerSecond) throws Exception {
        setupPhantomjsWithProxy(latencyMilliseconds,
                bandwidthKilobytePerSecond);
        try {
            String testName = "helloworld-empty-" + bandwidthKilobytePerSecond
                    + "kbps-" + latencyMilliseconds + "ms";
            runButtonTest(testName);
            runButtonTest(testName.replace("helloworld-empty-",
                    "helloworld-populated-"));
        } finally {
            stopProxyServer();
        }
    }

    private void runButtonTest(String testName) throws IOException {
        testBench().disableWaitForVaadin();

        LocalTime start = LocalTime.now();
        open();
        AtomicReference<WebElement> buttonHolder = new AtomicReference<WebElement>(
                null);
        waitUntil(new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver arg0) {

                List<WebElement> buttons = driver
                        .findElements(By.tagName("button"));
                if (buttons.isEmpty()) {
                    return false;
                }
                buttonHolder.set(buttons.get(0));
                return true;
            }
        });
        LocalTime end = LocalTime.now();
        long ms = ChronoUnit.MILLIS.between(start, end);

        printTeamcityStats(testName, ms);
        writeHar(proxyServer, testName);
    }

    private void stopProxyServer() {
        proxyServer.stop();
        proxyServer = null;
    }

    private void setupPhantomjsWithProxy(int latencyMilliseconds,
            int bandwidthKilobytePerSecond) {
        proxyServer = setupMobProxy(latencyMilliseconds,
                bandwidthKilobytePerSecond * 1024);
        // get the Selenium proxy object and configure it for the driver
        org.openqa.selenium.Proxy proxy = ClientUtil
                .createSeleniumProxy(proxyServer);
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(CapabilityType.PROXY, proxy);
        setupPhantomJsDriver(capabilities);

        // Start recording a new har file
        proxyServer.newHar("button");
    }

    protected static BrowserMobProxyServer setupMobProxy(long latencyMs,
            long bytesPerSecond) {
        BrowserMobProxyServer proxyServer = new net.lightbody.bmp.BrowserMobProxyServer();
        proxyServer.setHarCaptureTypes(CaptureType.REQUEST_HEADERS,
                CaptureType.RESPONSE_HEADERS);

        proxyServer.setWriteBandwidthLimit(bytesPerSecond);
        proxyServer.setReadBandwidthLimit(bytesPerSecond);
        proxyServer.setLatency(latencyMs, TimeUnit.MILLISECONDS);

        proxyServer.start();
        return proxyServer;
    }

    @Override
    protected String getRootURL() {
        // Can't use localhost or 127.0.0.1 because PhantomJS ignores the proxy
        // Waiting for https://github.com/ariya/phantomjs/pull/12703 to be
        // merged...
        try {
            String ip = Inet4Address.getLocalHost().getHostAddress();
            return "http://" + ip + ":" + SERVER_PORT;
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeHar(BrowserMobProxyServer proxyServer, String testName)
            throws IOException {
        Har har = proxyServer.getHar();
        FileOutputStream fos = new FileOutputStream(
                "target/" + testName + ".har");
        har.writeTo(fos);
    }

    public static void printTeamcityStats(String key, long value) {
        // ##teamcity[buildStatisticValue key=&#39;&lt;valueTypeKey&gt;&#39;
        // value=&#39;&lt;value&gt;&#39;]
        System.out.println("##teamcity[buildStatisticValue key='" + key
                + "' value='" + value + "']");

    }

}
