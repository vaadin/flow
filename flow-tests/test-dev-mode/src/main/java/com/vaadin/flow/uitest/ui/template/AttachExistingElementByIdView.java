/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route("com.vaadin.flow.uitest.ui.template.AttachExistingElementByIdView")
public class AttachExistingElementByIdView extends AbstractDivView {

    @JsModule("./AttachExistingElementById.js")
    @Tag("existing-element")
    public static class AttachExistingElementByIdTemplate
            extends AbstractAttachExistingElementByIdTemplate {

        AttachExistingElementByIdTemplate() {
            super("simple-path");
        }
    }

    public AttachExistingElementByIdView() {
        add(new AttachExistingElementByIdTemplate());
    }
}
