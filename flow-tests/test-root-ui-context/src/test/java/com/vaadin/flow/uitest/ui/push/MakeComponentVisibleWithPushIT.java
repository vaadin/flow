package com.vaadin.flow.uitest.ui.push;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.vaadin.flow.testcategory.PushTests;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

@Category(PushTests.class)
public class MakeComponentVisibleWithPushIT extends ChromeBrowserTest {

    @Test
    public void showingHiddenComponentByPushWorks() {
        open();

        $(TestBenchElement.class).id("update").click();
        Assert.assertEquals(
                "Unexpected value after updating the value when component is invisible",
                "foobar", $(TestBenchElement.class).id("input")
                        .getPropertyString("value"));
        $(TestBenchElement.class).id("update").click();
        Assert.assertEquals(
                "Unexpected value after updating the value when component is invisible",
                "foobarbar", $(TestBenchElement.class).id("input")
                        .getPropertyString("value"));

        checkLogsForErrors(
                msg -> msg.contains("sockjs-node") || msg.contains("[WDS]"));
    }
}