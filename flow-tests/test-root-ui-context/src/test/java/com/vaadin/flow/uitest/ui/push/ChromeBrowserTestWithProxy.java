package com.vaadin.flow.uitest.ui.push;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.junit.After;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.vaadin.flow.testcategory.PushTests;
import com.vaadin.flow.testutil.ChromeBrowserTest;

@Category(PushTests.class)
public abstract class ChromeBrowserTestWithProxy extends ChromeBrowserTest {

    private static AtomicInteger availablePort = new AtomicInteger(2000);
    private SimpleProxy proxySession;
    private Integer proxyPort = null;

    private String rootUrl;

    @Override
    public void setup() throws Exception {
        super.setup();
        connectProxy();
    }

    @After
    public void teardownProxy() {
        disconnectProxy();
    }

    @Override
    public void checkIfServerAvailable() {
        rootUrl = super.getRootURL();
        try {
            super.checkIfServerAvailable();
        } finally {
            rootUrl = null;
        }
    }

    @Override
    protected ChromeOptions customizeChromeOptions(ChromeOptions options) {
        ChromeOptions opts = super.customizeChromeOptions(options);

        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, Level.ALL);

        opts.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);
        return opts;
    }

    @Override
    protected List<DesiredCapabilities> customizeCapabilities(
            List<DesiredCapabilities> capabilities) {
        List<DesiredCapabilities> caps = super.customizeCapabilities(
                capabilities);
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, Level.ALL);
        caps.forEach(cap -> cap.setCapability(CapabilityType.LOGGING_PREFS,
                logPrefs));
        return caps;
    }

    protected Integer getProxyPort() {
        if (proxyPort == null) {
            // Assumes we can use any port >= 2000,
            // except for 2049 in Firefox...
            proxyPort = availablePort.addAndGet(1);
            if (proxyPort == 2049) {
                // Restricted in Firefox, see
                // http://www-archive.mozilla.org/projects/netlib/PortBanning.html#portlist
                proxyPort = availablePort.addAndGet(1);
            }
        }
        return proxyPort;
    }

    /**
     * Disconnects the proxy if active
     */
    protected void disconnectProxy() {
        if (proxySession == null) {
            return;
        }
        proxySession.disconnect();
        proxySession = null;
    }

    /**
     * Ensure the proxy is active. Does nothing if the proxy is already active.
     */
    protected void connectProxy() throws IOException {
        if (proxySession != null) {
            return;
        }
        for (int i = 0; i < 10; i++) {
            // Potential workaround for problem with establishing many ssh
            // connections at the same time
            try {
                createProxy(getProxyPort());
                break;
            } catch (IOException e) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException exception) {
                    new RuntimeException(exception);
                }
                if (i == 9) {
                    throw new RuntimeException(
                            "All 10 attempts to connect a proxy failed", e);
                }
            }
        }
    }

    private void createProxy(int proxyPort) throws IOException {
        proxySession = new SimpleProxy(proxyPort, getDeploymentHostname(),
                getDeploymentPort());
        proxySession.start();
    }

    @Override
    protected String getRootURL() {
        if (rootUrl != null) {
            return rootUrl;
        }
        return "http://" + getDeploymentHostname() + ":" + getProxyPort();
    }

}