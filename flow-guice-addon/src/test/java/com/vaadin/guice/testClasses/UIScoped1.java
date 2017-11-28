package com.vaadin.guice.testClasses;

import com.google.inject.Inject;

import com.vaadin.guice.annotation.UIScope;

@UIScope
public class UIScoped1 {

    @Inject
    private UIScoped2 uiScoped2;

    public UIScoped2 getUiScoped2() {
        return uiScoped2;
    }

    public void setUiScoped2(UIScoped2 uiScoped2) {
        this.uiScoped2 = uiScoped2;
    }
}
