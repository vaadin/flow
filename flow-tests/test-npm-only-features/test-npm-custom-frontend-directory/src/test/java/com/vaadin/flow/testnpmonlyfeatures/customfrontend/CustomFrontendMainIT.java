package com.vaadin.flow.testnpmonlyfeatures.customfrontend;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class CustomFrontendMainIT extends ChromeBrowserTest {
    @Test
    public void javascriptShouldHaveBeenExecuted() {
        open();
        Assert.assertNotNull($("div").id("executed"));
    }
}
