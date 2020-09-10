package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class TemplateAttributeIT extends ChromeBrowserTest {

    @Test
    public void readTemplateAttribiute() {
        open();

        TestBenchElement template = $(TestBenchElement.class).id("template");
        TestBenchElement info = template.$(TestBenchElement.class).id("info");
        Assert.assertEquals("foo bar", info.getText());
    }
}
