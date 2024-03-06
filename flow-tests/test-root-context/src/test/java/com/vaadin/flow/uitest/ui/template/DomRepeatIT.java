/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class DomRepeatIT extends ChromeBrowserTest {

    @Test
    public void checkThatIndicesAreCorrect() {
        open();

        TestBenchElement template = $(TestBenchElement.class).id("template");

        for (int i = 0; i < DomRepeatView.NUMBER_OF_EMPLOYEES; i++) {
            template.$(TestBenchElement.class)
                    .id(DomRepeatView.TR_ID_PREFIX + i).click();
            String eventIndex = template.$(TestBenchElement.class)
                    .id(DomRepeatView.EVENT_INDEX_ID).getText();
            String repeatIndex = template.$(TestBenchElement.class)
                    .id(DomRepeatView.REPEAT_INDEX_ID).getText();

            Assert.assertEquals(eventIndex, repeatIndex);
            Assert.assertEquals(i, Integer.parseInt(repeatIndex));
        }
    }
}
