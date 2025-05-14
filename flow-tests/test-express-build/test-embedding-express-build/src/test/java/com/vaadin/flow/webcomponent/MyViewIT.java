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
package com.vaadin.flow.webcomponent;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import static com.vaadin.flow.webcomponent.MyView.APP_TEXT_ID;

public class MyViewIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/com.vaadin.flow.webcomponent.MyView";
    }

    @Test
    public void applicationOpens_withEmbeddedComponents() {
        open();

        $(SpanElement.class).waitForFirst();
        SpanElement spanWithText = $(SpanElement.class).id(APP_TEXT_ID);

        Assert.assertTrue(
                spanWithText.getText().equals("This is the application view"));

        checkLogsForErrors();
    }

}
