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
import com.vaadin.flow.component.HtmlImport;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.templatemodel.TemplateModel;

@Tag("js-injected-grand-child")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/JsInjectedGrandChild.html")
public class JsInjectedGrandChild extends PolymerTemplate<TemplateModel> {

    public JsInjectedGrandChild() {
        getElement().callFunction("greet");
    }

    @ClientDelegate
    private void handleClientCall(String value) {
        getElement().setProperty("foo", value);
    }
}
