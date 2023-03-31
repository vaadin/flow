package com.vaadin.flow.eagerbootstrap;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ParameterIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/parameter";
    }

    @Test
    public void viewInitializedOnce() {
        openWithParameter("foo");
        int firstInstance = getInstance();
        openWithParameter("foo");
        Assert.assertEquals(firstInstance + 1, getInstance());
    }

    private void openWithParameter(String parameter) {
        String url = getTestURL();
        getDriver().get(url + "/" + parameter);
    }

    @Test
    public void setParameterCalledAsExpected() {
        openWithParameter("foo");
        Assert.assertEquals("setParameter called with: foo",
                getParametersText());
        $("*").id("barLink").click();
        Assert.assertEquals(
                "setParameter called with: foo\nsetParameter called with: bar",
                getParametersText());
    }

    private String getParametersText() {
        return $("*").id("parameters").getText();
    }

    private int getInstance() {
        String instanceText = $("*").id("instance").getText();
        return Integer
                .parseInt(instanceText.replace("This is view instance ", ""));
    }

}
