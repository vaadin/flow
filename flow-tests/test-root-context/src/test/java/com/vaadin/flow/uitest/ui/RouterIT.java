package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.flow.uitest.servlet.RouterTestServlet.ChildNavigationTarget;
import com.vaadin.flow.uitest.servlet.RouterTestServlet.FooBarNavigationTarget;
import com.vaadin.flow.uitest.servlet.RouterTestServlet.FooNavigationTarget;
import com.vaadin.flow.uitest.servlet.RouterTestServlet.RootNavigationTarget;

public class RouterIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/new-router-session/";
    }

    @Test
    public void rootNavigationTarget() {
        open();
        Assert.assertEquals(RootNavigationTarget.class.getSimpleName(),
                findElement(By.id("name-div")).getText());
    }

    @Test
    public void fooNavigationTarget() {
        openRouteUrl("foo");
        Assert.assertEquals(FooNavigationTarget.class.getSimpleName(),
                findElement(By.id("name-div")).getText());

        // Test that url with trailing slash also works
        openRouteUrl("foo/");
        Assert.assertEquals(FooNavigationTarget.class.getSimpleName(),
                findElement(By.id("name-div")).getText());
    }

    @Test
    public void fooBarNavigationTarget() {
        openRouteUrl("foo/bar");
        Assert.assertEquals(FooBarNavigationTarget.class.getSimpleName(),
                findElement(By.id("name-div")).getText());
    }

    @Test
    public void childIsInsideRouterLayout() {
        openRouteUrl("baz");

        Assert.assertTrue(isElementPresent(By.id("layout")));
        WebElement layout = findElement(By.id("layout"));

        Assert.assertEquals(ChildNavigationTarget.class.getSimpleName(),
                layout.findElement(By.id("name-div")).getText());
    }

    private void openRouteUrl(String route) {
        getDriver().get(getRootURL() + getTestPath() + route);
    }
}
