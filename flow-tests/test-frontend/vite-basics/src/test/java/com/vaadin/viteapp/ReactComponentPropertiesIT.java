package com.vaadin.viteapp;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Map;

public class ReactComponentPropertiesIT extends ChromeBrowserTest {

    private static final String DATA_TEST_ID = "data-test-id";

    @Before
    public void openView() {
        getDriver().get(getTestURL());
        waitForDevServer();
    }

    @Override
    protected String getTestPath() {
        return "/react-component-properties";
    }

    @Test
    public void inlinePropsCanFound() {
        var componentWithInlinePropsElement = $("*")
                .withAttribute(DATA_TEST_ID, "component-with-inline-props")
                .waitForFirst();
        Map<String, Object> debugPropertiesFromFiberNode = getDebugPropertiesFromFiberNode(
                componentWithInlinePropsElement);
        var error = (Boolean) debugPropertiesFromFiberNode.get("error");
        Assert.assertFalse(error);
        debugPropertiesFromFiberNode.get("properties");
        List<?> properties = (List<?>) debugPropertiesFromFiberNode
                .get("properties");
        Assert.assertFalse(properties.isEmpty());
    }

    @Test
    public void intrinsicPropsCanFoundInWindow() {
        var h1 = $("*").withAttribute(DATA_TEST_ID, "simple-header")
                .waitForFirst();
        Map<String, Object> debugPropertiesFromFiberNode = getDebugPropertiesFromFiberNode(
                h1);
        Assert.assertNull(debugPropertiesFromFiberNode);
        Map<String, Object> debugPropertiesFromWindow = getDebugPropertiesFromWindow(
                h1);
        Assert.assertNotNull(debugPropertiesFromWindow);
        Assert.assertFalse((Boolean) debugPropertiesFromWindow.get("error"));
        Assert.assertFalse(
                ((List<?>) debugPropertiesFromWindow.get("properties"))
                        .isEmpty());
    }

    @Test
    public void interfacePropsCanFound() {
        var compWithInterfaceProps = $("*")
                .withAttribute(DATA_TEST_ID, "component-with-interface-props")
                .waitForFirst();
        Map<String, Object> debugPropertiesFromFiberNode = getDebugPropertiesFromFiberNode(
                compWithInterfaceProps);
        Assert.assertNotNull(debugPropertiesFromFiberNode);
        assertError(debugPropertiesFromFiberNode, false);
    }

    @Test
    public void componentWithoutPropsShouldHaveAnError() {
        var element = $("*")
                .withAttribute(DATA_TEST_ID, "component-with-any-props")
                .waitForFirst();
        Assert.assertNull(getDebugPropertiesFromFiberNode(element));
        Map<String, Object> debugPropertiesFromWindow = getDebugPropertiesFromWindow(
                element);
        assertError(debugPropertiesFromWindow, true);
    }

    @Test
    public void errorShouldBeRegisteredWhenIntrinsicElementsNotLoaded() {
        var anySpan = $("span").waitForFirst();
        Map<String, Object> spanProps = getDebugPropertiesFromWindow(anySpan);
        assertError(spanProps, true);

        var anyDiv = $("div").waitForFirst();
        Map<String, Object> divProperties = getDebugPropertiesFromWindow(
                anyDiv);
        assertError(divProperties, false);
    }
    @Test
    public void componentWithAliasShouldWork(){
        var element = $("*")
                .withAttribute(DATA_TEST_ID, "basic-react-component")
                .waitForFirst();
        Map<String, Object> debugPropertiesFromFiberNode = getDebugPropertiesFromFiberNode(element);
        assertError(debugPropertiesFromFiberNode, false);
        List<?> properties = (List<?>) debugPropertiesFromFiberNode
                .get("properties");
        Assert.assertEquals(1, properties.size());
        Map<String, ?> propertyValues = (Map<String, ?>) properties.get(0);
        Assert.assertEquals("Name", propertyValues.get("label"));

    }

    private void assertError(Map<String, Object> propObj, boolean expected) {
        var error = (Boolean) propObj.get("error");
        Assert.assertEquals(expected, error);
    }

    private Map<String, Object> getDebugPropertiesFromWindow(
            WebElement element) {
        String tagName = element.getTagName();
        return (Map<String, Object>) executeScript(
                """
                        return window.Vaadin.copilot.ReactProperties.properties[arguments[0]];
                        """,
                tagName);
    }

    private Map<String, Object> getDebugPropertiesFromFiberNode(
            WebElement element) {
        return (Map<String, Object>) executeScript(
                """
                        const key = Object.keys(arguments[0]).filter(a => a.startsWith("__reactFiber"))[0];
                        const fiber = arguments[0][key];
                        return fiber.return.type?.__debugProperties;
                        """,
                element);
    }

}
