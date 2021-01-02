package com.vaadin.flow.component.html.testbench;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NativeDetailsElementIT extends ChromeBrowserTest {

    private NativeDetailsElement details;
    private DivElement log;

    @Before
    public void open() {
        getDriver().get("http://localhost:8888/Details");
        details = $(NativeDetailsElement.class).id("details");
        log = $(DivElement.class).id("log");
    }

    @Test
    public void openDetails() {
        details.setProperty("open", true);
        Assert.assertEquals("Toggle event is 'true'", log.getText());
    }

    @Test
    public void openAndCloseDetails() {
        details.setProperty("open", true);
        Assert.assertEquals("Toggle event is 'true'", log.getText());

        details.setProperty("open", false);
        Assert.assertEquals("Toggle event is 'false'", log.getText());
    }

    @Test
    public void closingAlreadyClosedDetails() {
        details.setProperty("open", false);
        // Event should not be triggered, because details open property
        // defaults to false
        Assert.assertEquals("", log.getText());
    }
}
