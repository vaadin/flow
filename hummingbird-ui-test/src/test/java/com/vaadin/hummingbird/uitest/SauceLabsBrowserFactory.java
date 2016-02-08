package com.vaadin.hummingbird.uitest;

import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.vaadin.testbench.parallel.Browser;
import com.vaadin.testbench.parallel.TestBenchBrowserFactory;

public class SauceLabsBrowserFactory implements TestBenchBrowserFactory {

    private static final String EDGE_VERSION = "20.10240";
    private static final String CHROME_VERSION = "48.0";
    private static final String WINDOWS_10 = "Windows 10";

    @Override
    public DesiredCapabilities create(Browser browser) {
        if (browser == Browser.CHROME) {
            DesiredCapabilities caps = DesiredCapabilities.chrome();
            caps.setCapability("platform", WINDOWS_10);
            caps.setCapability("version", CHROME_VERSION);
            return caps;
        } else if (browser == Browser.EDGE) {
            DesiredCapabilities caps = DesiredCapabilities.edge();
            caps.setCapability("platform", WINDOWS_10);
            caps.setCapability("version", EDGE_VERSION);
            return caps;
        } else if (browser == Browser.FIREFOX) {
            DesiredCapabilities caps = DesiredCapabilities.firefox();
            caps.setCapability("platform", WINDOWS_10);
            caps.setCapability("version", "44.0");
            return caps;
        } else if (browser == Browser.IE11) {
            DesiredCapabilities caps = DesiredCapabilities.internetExplorer();
            caps.setCapability("platform", WINDOWS_10);
            caps.setCapability("version", "11.0");
            return caps;
        } else if (browser == Browser.SAFARI) {
            DesiredCapabilities caps = DesiredCapabilities.safari();
            caps.setCapability("platform", "OS X 10.11");
            caps.setCapability("version", "9.0");
            return caps;
        }
        throw new IllegalArgumentException("No support for " + browser
                + " implemented in " + getClass().getName());
    }

    @Override
    public DesiredCapabilities create(Browser browser, String version) {
        return create(browser);
    }

    @Override
    public DesiredCapabilities create(Browser browser, String version,
            Platform platform) {
        return create(browser);
    }

    public DesiredCapabilities createAndroid51Phone() {
        // See https://wiki.saucelabs.com/display/DOCS/Platform+Configurator#/
        DesiredCapabilities caps = DesiredCapabilities.android();
        caps.setCapability("platform", "Linux");
        caps.setCapability("version", "5.1");
        caps.setCapability("deviceName", "Android Emulator");
        caps.setCapability("deviceType", "phone");
        caps.setCapability("deviceOrientation", "portrait");
        return caps;
    }

    public DesiredCapabilities createIphone6Ios92() {
        // See https://wiki.saucelabs.com/display/DOCS/Platform+Configurator#/
        DesiredCapabilities caps = DesiredCapabilities.iphone();
        caps.setCapability("platform", "OS X 10.10");
        caps.setCapability("version", "9.2");
        caps.setCapability("deviceName", "iPhone 6");
        caps.setCapability("deviceOrientation", "portrait");
        return caps;
    }

}
