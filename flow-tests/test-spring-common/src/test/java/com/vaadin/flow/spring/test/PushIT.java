package com.vaadin.flow.spring.test;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class PushIT extends ChromeBrowserTest {

    @Test
    public void websocketsWork() throws Exception {
        open();
        $("button").first().click();
        Thread.sleep(1000);
        List<TestBenchElement> paragraphs = $("p").all();
        Assert.assertEquals(2, paragraphs.size());
        Assert.assertEquals("Hello", paragraphs.get(0).getText());
        Assert.assertEquals("World", paragraphs.get(1).getText());
    }

    @Override
    protected String getTestPath() {
        return "/push";
    }
}
