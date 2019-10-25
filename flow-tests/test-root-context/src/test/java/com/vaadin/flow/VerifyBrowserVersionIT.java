package com.vaadin.flow;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.parallel.BrowserUtil;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class VerifyBrowserVersionIT extends ChromeBrowserTest {

    @Test
    public void verifyUserAgent() {
        open();

        DesiredCapabilities desiredCapabilities = getDesiredCapabilities();

        String userAgent = findElement(By.id("userAgent")).getText();
        String browserIdentifier;

        if (BrowserUtil.isChrome(getDesiredCapabilities())) {
            // Chrome version does not necessarily match the desired version
            // because of auto updates...
            browserIdentifier = getExpectedUserAgentString(
                    getDesiredCapabilities()) + "78";
        } else if (BrowserUtil.isFirefox(getDesiredCapabilities())) {
            browserIdentifier = getExpectedUserAgentString(
                    getDesiredCapabilities()) + "58";
        } else {
            browserIdentifier = getExpectedUserAgentString(desiredCapabilities)
                    + desiredCapabilities.getVersion();
        }

        assertThat(userAgent, containsString(browserIdentifier));
    }

    private String getExpectedUserAgentString(DesiredCapabilities dCap) {
        if (BrowserUtil.isIE(dCap)) {
            // IE11
            return "Trident/7.0; rv:";
        } else if (BrowserUtil.isFirefox(dCap)) {
            return "Firefox/";
        } else if (BrowserUtil.isChrome(dCap)) {
            return "Chrome/";
        }
        throw new UnsupportedOperationException(
                "Test is being run on unknown browser.");
    }

}
