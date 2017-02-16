package com.vaadin.hummingbird.demo.expensemanager.views;

import com.vaadin.annotations.Id;
import com.vaadin.hummingbird.components.CustomStyle;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.ui.Template;

public class MainLayout extends Template {

    @Id("custom-styles")
    private Div customStylesWrapper;

    public MainLayout() {
        customStylesWrapper.getElement().appendChild(CustomStyle.getAppStyles().getElement());
    }

}
