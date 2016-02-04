package com.vaadin.hummingbird.uitest;

import org.junit.Assert;
import org.junit.Test;

public class PhantomJSVersionIT extends PhantomJSTest {

    @Test
    public void checkPhantomJsVersion() {
        String userAgent = (String) executeScript(
                "return navigator.userAgent;");
        // Mozilla/5.0 (Macintosh; Intel Mac OS X) AppleWebKit/538.1 (KHTML,
        // like Gecko) PhantomJS/2.1.1 Safari/538.1
        Assert.assertTrue(userAgent.contains(" PhantomJS/2.1."));
    }
}
