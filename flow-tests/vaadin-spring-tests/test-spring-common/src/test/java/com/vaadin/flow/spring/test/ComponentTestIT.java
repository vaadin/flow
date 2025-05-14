/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.spring.test;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.notification.testbench.NotificationElement;

public class ComponentTestIT extends AbstractSpringTest {

    @Test
    public void componentsAreFoundAndLoaded() throws Exception {
        open();

        $(ButtonElement.class).waitForFirst();

        $(ButtonElement.class).first().click();

        Assert.assertTrue(
                "Clicking button should have opened a notification successfully.",
                $(NotificationElement.class).exists());
    }

    @Override
    protected String getTestPath() {
        return "/component-test";
    }

}
