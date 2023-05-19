package com.vaadin.viteapp;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ProductionBasicsIT extends ChromeBrowserTest {

    @Test
    public void applicationStarts() {
        getDriver().get(getRootURL());
        waitForDevServer();
        TestBenchElement testForm = $("test-form").first();
        Assert.assertEquals("foo", testForm.getText());
    }

}
