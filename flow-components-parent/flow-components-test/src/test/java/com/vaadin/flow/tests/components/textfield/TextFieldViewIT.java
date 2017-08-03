/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.tests.components.textfield;

import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.tests.components.AbstractComponentIT;
import com.vaadin.testbench.By;
import com.vaadin.ui.VaadinTextField;

/**
 * Integration tests for {@link VaadinTextField}.
 */
public class TextFieldViewIT extends AbstractComponentIT {

    @Before
    public void init() {
        open();
    }

    @Test
    public void pass() {
        isElementPresent(By.tagName("vaadin-text-field"));
    }
}
