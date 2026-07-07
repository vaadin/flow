/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.test.ui;

import org.junit.jupiter.api.Assertions;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

@TestFor(RemoveAddVisibilityView.class)
public class RemoveAddVisibilityIT extends AbstractDefaultIT {

    @BrowserTest
    void elementIsVisibleAfterReattach() {
        open();

        SpanElement span = $(SpanElement.class).first();
        Assertions.assertEquals(Boolean.TRUE.toString(),
                span.getAttribute("hidden"));

        $(NativeButtonElement.class).id("make-visible").click();

        Assertions.assertNull(span.getAttribute("hidden"));
        Assertions.assertEquals("Initially hidden", span.getText());
    }
}
