/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.littemplate;

public class SimpleLitTemplateNoShadowRootIT
        extends SimpleLitTemplateShadowRootIT {

    protected String getTemplateTag() {
        return "simple-lit-template-no-shadow-root";
    }

    @Override
    protected boolean shouldHaveShadowRoot() {
        return false;
    }

}
