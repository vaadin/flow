/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;

@Tag("template-with-connected-callbacks")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/TemplateWithConnectedCallbacks.html")
@JsModule("TemplateWithConnectedCallbacks.js")
public class TemplateWithConnectedCallbacks extends Component {

    public TemplateWithConnectedCallbacks() {
        getElement().synchronizeProperty("connected", "connected-changed");

        getElement().addPropertyChangeListener("connected", evt -> {
            if (evt.isUserOriginated()) {
                setConnected("Connected (checked from server side)");
            }
        });
    }

    public String getConnected() {
        return getElement().getProperty("connected");
    }

    public void setConnected(String connected) {
        getElement().setProperty("connected", connected);
    }
}
