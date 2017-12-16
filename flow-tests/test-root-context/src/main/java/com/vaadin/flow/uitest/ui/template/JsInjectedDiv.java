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
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.ClientDelegate;
import com.vaadin.flow.component.JavaScript;
import com.vaadin.flow.component.html.Div;

@JavaScript("frontend://divConnector.js")
public class JsInjectedDiv extends Div {

    public JsInjectedDiv() {
        getElement().getNode()
                .runWhenAttached(ui -> ui.getPage().executeJavaScript(
                        "window.divConnector.jsFunction($0)", getElement()));
    }

    @ClientDelegate
    private void handleClientCall(String value) {
        setText(value);
    }
}
