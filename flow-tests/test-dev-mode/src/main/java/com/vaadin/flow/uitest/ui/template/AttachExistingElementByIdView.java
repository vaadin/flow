/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import com.vaadin.flow.uitest.ui.AbstractDivView;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.template.AttachExistingElementByIdView")
public class AttachExistingElementByIdView extends AbstractDivView {

    @HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/AttachExistingElementById.html")
    @Tag("existing-element")
    public static class AttachExistingElementByIdTemplate
            extends AbstractAttachExistingElementByIdTemplate {

        AttachExistingElementByIdTemplate() {
            super("simple-path");
        }
    }

    @HtmlImport("context://frontend/com/vaadin/flow/uitest/ui/template/ContextAttachExistingElementById.html")
    @Tag("context-existing-element")
    public static class ContextAttachExistingElementByIdTemplate
            extends AbstractAttachExistingElementByIdTemplate {

        ContextAttachExistingElementByIdTemplate() {
            super("context-path");
        }
    }

    @HtmlImport("components/AttachExistingElementById.html")
    @Tag("frontend-existing-element")
    public static class FrontendAttachExistingElementByIdTemplate
            extends AbstractAttachExistingElementByIdTemplate {

        FrontendAttachExistingElementByIdTemplate() {
            super("frontend-path");
        }
    }

    public AttachExistingElementByIdView() {
        add(new AttachExistingElementByIdTemplate(),
                new ContextAttachExistingElementByIdTemplate(),
                new FrontendAttachExistingElementByIdTemplate());
    }
}
