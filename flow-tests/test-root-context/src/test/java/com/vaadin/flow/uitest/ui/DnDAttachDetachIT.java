/*
 * Copyright 2000-2019 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import static com.vaadin.flow.uitest.ui.DnDAttachDetachView.DRAGGABLE_ID;
import static com.vaadin.flow.uitest.ui.DnDAttachDetachView.MOVE_BUTTON_ID;
import static com.vaadin.flow.uitest.ui.DnDAttachDetachView.SWAP_BUTTON_ID;
import static com.vaadin.flow.uitest.ui.DnDAttachDetachView.VIEW_1_ID;
import static com.vaadin.flow.uitest.ui.DnDAttachDetachView.VIEW_2_ID;

public class DnDAttachDetachIT extends ChromeBrowserTest {

    /* https://github.com/vaadin/flow/issues/6054 */
    @Test
    public void testDnD_attachDetachAttachSourceAndTarget_dndOperationWorks() {
        open();

        dragAndDrop(getDraggableText(), getDropTarget());

        TestBenchElement eventElement = getEvent(0);
        Assert.assertEquals("Drop: 0", eventElement.getText());

        clickElementWithJs(SWAP_BUTTON_ID);

        // just verify that the component was removed
        waitForElementNotPresent(By.id(VIEW_1_ID));
        waitForElementPresent(By.id(VIEW_2_ID));

        clickElementWithJs(SWAP_BUTTON_ID);

        dragAndDrop(getDraggableText(), getDropTarget());

        // without proper reactivation of the drop target, the following event
        // is not discoved
        eventElement = getEvent(1);
        Assert.assertEquals("Drop: 1", eventElement.getText());

        Assert.assertFalse("No second event should have occurred",
                isElementPresent(By.id("drop-" + 2)));
    }

    @Test
    public void testDnD_moveComponents_dndOperationWorks() {
        open();

        dragAndDrop(getDraggableText(), getDropTarget());

        TestBenchElement eventElement = getEvent(0);
        Assert.assertEquals("Drop: 0", eventElement.getText());

        clickElementWithJs(MOVE_BUTTON_ID);

        dragAndDrop(getDraggableText(), getDropTarget());

        eventElement = getEvent(1);
        Assert.assertEquals("Drop: 1", eventElement.getText());

        Assert.assertFalse("No second event should have occurred",
                isElementPresent(By.id("drop-" + 2)));
    }

    private TestBenchElement getEvent(int i) {
        waitForElementPresent(By.id("drop-" + i));
        return $(TestBenchElement.class).id("drop-" + i);
    }

    private TestBenchElement getDraggableText() {
        return id(DRAGGABLE_ID);
    }

    private TestBenchElement getDropTarget() {
        return id(VIEW_1_ID);
    }

    private TestBenchElement id(String id) {
        return $(TestBenchElement.class).id(id);
    }
}
