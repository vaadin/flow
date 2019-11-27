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

import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class OneWayPolymerBindingIT extends ChromeBrowserTest {

    // Numerous tests are carried out in the single test case, because it's
    // expensive to launch numerous Chrome instances
    @Test
    public void initialModelValueIsPresentAndModelUpdatesNormally() {
        open();

        TestBenchElement template = $(TestBenchElement.class).id("template");

        checkInitialState(template);
        checkTemplateModel(template);

        template.$(TestBenchElement.class).id("changeModelValue").click();

        checkStateAfterClick(template);
        checkTemplateModel(template);
    }

    private void checkInitialState(TestBenchElement template) {
        String messageDivText = template.$(TestBenchElement.class)
                .id("messageDiv").getText();
        String titleDivText = template.$(TestBenchElement.class).id("titleDiv")
                .getText();
        Assert.assertEquals(OneWayPolymerBindingView.MESSAGE, messageDivText);
        Assert.assertEquals("", titleDivText);
    }

    private void checkTemplateModel(TestBenchElement template) {
        assertTrue(template.$(TestBenchElement.class)
                .attribute("id", "titleDivConditional").all().size() > 0);
        Assert.assertEquals(0, template.$(TestBenchElement.class)
                .attribute("id", "nonExistingProperty").all().size());
    }

    private void checkStateAfterClick(TestBenchElement template) {
        String changedMessageDivText = template.$(TestBenchElement.class)
                .id("messageDiv").getText();
        String titleDivText = template.$(TestBenchElement.class).id("titleDiv")
                .getText();

        Assert.assertEquals(OneWayPolymerBindingView.NEW_MESSAGE,
                changedMessageDivText);
        Assert.assertEquals("", titleDivText);
    }
}
