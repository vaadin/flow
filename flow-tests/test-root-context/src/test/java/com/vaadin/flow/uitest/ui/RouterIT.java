package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.flow.uitest.servlet.RouterTestServlet;
import com.vaadin.flow.uitest.servlet.RouterTestServlet.ChildNavigationTarget;
import com.vaadin.flow.uitest.servlet.RouterTestServlet.FooBarNavigationTarget;
import com.vaadin.flow.uitest.servlet.RouterTestServlet.FooNavigationTarget;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

public class RouterIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/new-router-session/";
    }

    @Test
    public void rootNavigationTarget() {
        open();
        Assert.assertEquals(
                ViewTestLayout.BaseNavigationTarget.class.getSimpleName(),
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

    @Test
    public void stringRouteParameter() {
        openRouteUrl("greeting/World");
        Assert.assertEquals("Hello, World!",
                findElement(By.id("greeting-div")).getText());
    }

    @Test
    public void targetHasMultipleParentLayouts() {
        openRouteUrl("target");

        Assert.assertTrue("Missing top most level: main layout",
                isElementPresent(By.id("mainLayout")));
        Assert.assertTrue("Missing center layout: middle layout",
                isElementPresent(By.id("middleLayout")));

        WebElement layout = findElement(By.id("middleLayout"));

        Assert.assertEquals("Child layout is the wrong class",
                RouterTestServlet.TargetLayout.class.getSimpleName(),
                layout.findElement(By.id("name-div")).getText());
    }

    @Test
    public void faultyRouteShowsExpectedErrorScreen() {
        openRouteUrl("exception");

        WebElement element = findElement(By.id("error-path"));
        Assert.assertNotNull(element);
        Assert.assertEquals("exception", element.getText());
    }

    private void openRouteUrl(String route) {
        getDriver().get(getRootURL() + getTestPath() + route);
    }
}
