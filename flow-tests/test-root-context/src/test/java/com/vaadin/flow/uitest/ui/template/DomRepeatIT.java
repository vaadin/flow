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

package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.vaadin.flow.testcategory.IgnoreOSGi;
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
