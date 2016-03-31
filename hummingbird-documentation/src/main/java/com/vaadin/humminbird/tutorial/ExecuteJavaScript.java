/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.humminbird.tutorial;

import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.ui.Page;
import com.vaadin.ui.UI;

@CodeFor("tutorial-execute-javascript.asciidoc")
public class ExecuteJavaScript {
    public static void logElementSize(String name, Element element) {
        Page page = UI.getCurrent().getPage();

        page.executeJavaScript(
                "console.log($0 + ' size:', $1.offsetWidth, $1.offsetHeight)",
                name, element);
    }
}
