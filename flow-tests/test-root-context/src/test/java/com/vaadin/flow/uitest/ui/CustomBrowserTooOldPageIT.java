package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class CustomBrowserTooOldPageIT extends ChromeBrowserTest {

    @Test
    public void customPageUsed() {
        getDriver().get(getRootURL() + "/view/?v-r=browser-too-old");
        Assert.assertTrue(getDriver().getPageSource()
                .contains("You so old you cannot view this page"));
    }

}
