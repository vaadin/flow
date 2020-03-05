package com.vaadin.flow.uitest.ui.push;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.vaadin.flow.testcategory.PushTests;

@Category(PushTests.class)
public class PushFromInitIT extends AbstractLogTest {
    @Test
    public void pushFromInit() {
        open();

        waitUntil(driver -> ("3. " + PushFromInitUI.LOG_AFTER_INIT)
                .equals(getLastLog().getText()));

    }
}