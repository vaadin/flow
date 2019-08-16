package com.vaadin.flow.component.page;

import org.junit.Test;
import org.junit.Assert;

public class ExtendedClientDetailsTest {

    @Test
    public void initializeWithClientValues_gettersReturnExpectedValues() {
        final ExtendedClientDetails details = new ExtendedClientDetails(
                "2560",
                "1450",
                "2400",
                "1400",
                "1600",
                "1360",
                "-270", // minutes from UTC
                "-210", // minutes from UTC without DST
                "60", // dist shift amount
                "true",
                "Asia/Tehran",
                "1555000000000", // Apr 11 2019
                "false",
                "2.0",
                "ROOT-1234567-0.1234567");
        Assert.assertEquals(2560, details.getScreenWidth());
        Assert.assertEquals(1450, details.getScreenHeight());
        Assert.assertEquals(2400, details.getWindowInnerWidth());
        Assert.assertEquals(1400, details.getWindowInnerHeight());
        Assert.assertEquals(1600, details.getBodyClientWidth());
        Assert.assertEquals(1360, details.getBodyClientHeight());
        Assert.assertEquals(16200000, details.getTimezoneOffset());
        Assert.assertEquals("Asia/Tehran", details.getTimeZoneId());
        Assert.assertEquals(12600000, details.getRawTimezoneOffset());
        Assert.assertEquals(3600000, details.getDSTSavings());
        Assert.assertEquals(true, details.isDSTInEffect());
        Assert.assertEquals(false, details.isTouchDevice());
        Assert.assertEquals(2.0D, details.getDevicePixelRatio(), 0.0);
        Assert.assertEquals("ROOT-1234567-0.1234567", details.getWindowName());

        // Don't test getCurrentDate() and time delta due to the dependency on
        // server-side time
    }
}
