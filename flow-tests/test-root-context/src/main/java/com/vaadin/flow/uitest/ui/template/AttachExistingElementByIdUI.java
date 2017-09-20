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

import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.event.Tag;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

public class AttachExistingElementByIdUI extends UI {

    @HtmlImport("/com/vaadin/flow/uitest/ui/template/AttachExistingElementById.html")
    @Tag("existing-element")
    public static class AttachExistingElementByIdTemplate
            extends AbstractAttachExistingElementByIdTemplate {

        AttachExistingElementByIdTemplate() {
            super("simple-path");
        }
    }

    @HtmlImport("context://com/vaadin/flow/uitest/ui/template/ContextAttachExistingElementById.html")
    @Tag("context-existing-element")
    public static class ContextAttachExistingElementByIdTemplate
            extends AbstractAttachExistingElementByIdTemplate {

        ContextAttachExistingElementByIdTemplate() {
            super("context-path");
        }
    }

    @HtmlImport("frontend://components/AttachExistingElementById.html")
    @Tag("frontend-existing-element")
    public static class FrontendAttachExistingElementByIdTemplate
            extends AbstractAttachExistingElementByIdTemplate {

        FrontendAttachExistingElementByIdTemplate() {
            super("frontend-path");
        }
    }

    @Override
    protected void init(VaadinRequest request) {
        add(new AttachExistingElementByIdTemplate(),
                new ContextAttachExistingElementByIdTemplate(),
                new FrontendAttachExistingElementByIdTemplate());
    }
}
