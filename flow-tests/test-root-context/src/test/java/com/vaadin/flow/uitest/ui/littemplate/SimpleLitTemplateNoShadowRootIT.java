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
