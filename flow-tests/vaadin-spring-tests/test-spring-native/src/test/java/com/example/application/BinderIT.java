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
package com.example.application;

import org.junit.Test;

import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BinderIT extends ChromeBrowserTest {

    @Test
    public void writeBean_propertiesWritten() {
        open();

        $(InputTextElement.class).id(BinderView.NAME_ID).setValue("Jane");
        $(InputTextElement.class).id(BinderView.AGE_ID).setValue("42");
        $(NativeButtonElement.class).id(BinderView.SAVE_ID).click();

        SpanElement result = $(SpanElement.class).id(BinderView.RESULT_ID);
        waitUntil(driver -> "Jane is 42".equals(result.getText()));
    }

    @Override
    protected String getTestPath() {
        return "/binder";
    }
}
