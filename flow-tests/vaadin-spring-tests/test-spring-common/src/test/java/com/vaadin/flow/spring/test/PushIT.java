package com.vaadin.flow.spring.test;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.testbench.TestBenchElement;

public class PushIT extends AbstractSpringTest {

    @Test
    public void websocketsWork() throws Exception {
        open();
        $("button").first().click();
        TestBenchElement world = $("p").withAttribute("id", "world")
                .waitForFirst();
        Assert.assertEquals("World", world.getText());
    }

    @Override
    protected String getTestPath() {
        return "/push";
    }
}
