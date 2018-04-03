package com.vaadin.flow.spring.test;

import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class TemplatePushIT extends ChromeBrowserTest {

    @Test
    public void elementChangesPushed() throws Exception {
        open();
        TestBenchElement tpl = $("template-push-view").first();
        tpl.$("button").id("elementTest").click();
        TestBenchElement label = tpl.$("label").id("label");
        waitUntil(foo -> {
            return "from Element API".equals(label.getText());
        }, 5);
    }

    @Test
    public void execJsPushed() throws Exception {
        open();
        TestBenchElement tpl = $("template-push-view").first();
        tpl.$("button").id("execJsTest").click();
        TestBenchElement label = tpl.$("label").id("label");
        waitUntil(foo -> {
            return "from execJS".equals(label.getText());
        }, 5);
    }

    @Override
    protected String getTestPath() {
        return "/template-push";
    }
}
