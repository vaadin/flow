package com.vaadin.flow.uitest.ui.push;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;

import com.vaadin.flow.testutil.TestTag;
import com.vaadin.flow.testutil.jupiter.ChromeBrowserTest;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.TestBenchElement;

@Tag(TestTag.PUSH_TESTS)
public class MakeComponentVisibleWithPushIT extends ChromeBrowserTest {

    @BrowserTest
    public void showingHiddenComponentByPushWorks() {
        open();

        $(TestBenchElement.class).id("update").click();
        Assertions.assertEquals(
                "Unexpected value after updating the value when component is invisible",
                "foobar", $(TestBenchElement.class).id("input")
                        .getPropertyString("value"));
        $(TestBenchElement.class).id("update").click();
        Assertions.assertEquals(
                "Unexpected value after updating the value when component is invisible",
                "foobarbar", $(TestBenchElement.class).id("input")
                        .getPropertyString("value"));

        checkLogsForErrors(
                msg -> msg.contains("sockjs-node") || msg.contains("[WDS]"));
    }
}
