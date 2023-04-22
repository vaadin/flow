package com.vaadin.viteapp;

import org.junit.Before;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import org.junit.Assert;
import org.junit.Test;

public class ContextPathIT extends ChromeBrowserTest {

    @Before
    public void openView() {
        getDriver().get(getRootURL() + "/my-context");
        waitForDevServer();
        getCommandExecutor().waitForVaadin();
    }

    @Test
    public void applicationStarts() {
        TestBenchElement header = $("h2").first();
        Assert.assertEquals("Hello world!", header.getText());
    }
}
