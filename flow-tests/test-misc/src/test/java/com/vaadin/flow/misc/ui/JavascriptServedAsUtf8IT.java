package com.vaadin.flow.misc.ui;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class JavascriptServedAsUtf8IT extends ChromeBrowserTest {

    @Test
    public void loadJavascriptWithUtf8() {
        getDriver().get(getRootURL() + "/frontend/test-files/js/unicode.js");
        String source = getDriver().getPageSource();
        Assert.assertTrue(
                "Page should have contained umlaut characters but contained: "
                        + source,
                source.contains("åäöü°"));
    }
}
