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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.ScriptInjectView", layout = ViewTestLayout.class)
public class ScriptInjectView extends AbstractDivView {

    static String[] values = new String[] { "</script foo>", "</Script>",
            "</SCRIPT >", "</SCRIPT>", "< / SCRIPT>", "</ SCRIPT>",
            "< / SCRIPT >", "</SCRIpT>" };

    public ScriptInjectView() {
        for (String value : values) {
            createInput(value);
        }
    }

    private void createInput(String endTag) {
        String string = getValue(endTag);
        Element input = ElementFactory.createInput();
        input.setAttribute("value", string);
        getElement().appendChild(input);
    }

    static String getValue(String endTag) {
        return endTag + "<script>alert('foo');>" + endTag;
    }
}
