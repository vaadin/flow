package com.vaadin.flow.uitest.ui.push;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.vaadin.flow.testcategory.PushTests;

@Category(PushTests.class)
public class TrackMessageSizeIT extends AbstractLogTest {
    @Test
    public void runTests() {
        open();

        Assert.assertEquals("1. All tests run", getLastLog().getText());
    }
}
