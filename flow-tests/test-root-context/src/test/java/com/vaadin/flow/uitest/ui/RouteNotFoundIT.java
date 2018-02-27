package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class RouteNotFoundIT extends ChromeBrowserTest {

    @Test
    public void notFoundDevMode() {
        getDriver().get(getRootURL() + "/view/notfound");
        String pageSource = getDriver().getPageSource();
        Assert.assertTrue(pageSource.contains("Available routes"));
        Assert.assertTrue(pageSource.contains("noParent"));
        Assert.assertTrue(pageSource.contains("foo/bar"));
    }

    @Test
    public void notFoundProdMode() {
        getDriver().get(getRootURL() + "/view-production/notfound");
        String pageSource = getDriver().getPageSource();
        Assert.assertFalse(pageSource.contains("Available routes"));
        Assert.assertFalse(pageSource.contains("noParent"));
        Assert.assertFalse(pageSource.contains("foo/bar"));
    }

}
