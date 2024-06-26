/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
