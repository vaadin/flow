package com.vaadin.flow.component.html.testbench;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RangeInputElementIT extends ChromeBrowserTest {

    private RangeInputElement input;
    private DivElement log;

    @Before
    public void open() {
        getDriver().get("http://localhost:8888/RangeInput");
        input = $(RangeInputElement.class).id("input");
        log = $(DivElement.class).id("log");
    }

    @Test
    public void getSetValue() {
        Assert.assertNull(input.getValue());
        input.setValue(5.0);
        Assert.assertEquals(5.0, (double) input.getValue(), 0.1);
        Assert.assertEquals("Value is '5.0'", log.getText());
    }

    @Test
    public void setValueEmpty() {
        input.setValue(6.0);
        input.setValue(null);
        Assert.assertNull(input.getValue());
        Assert.assertEquals("Value is 'null'", log.getText());
    }

    @Test
    public void clearEmpty() {
        input.clear();
        Assert.assertNull(input.getValue());
        Assert.assertEquals("", log.getText());
    }

    @Test
    public void clearWithValue() {
        input.setValue(7.0);
        input.clear();
        Assert.assertNull(input.getValue());
        Assert.assertEquals("Value is 'null'", log.getText());
    }
}
