package com.vaadin.flow.demo.expensemanager.views;

import com.vaadin.annotations.Id;
import com.vaadin.flow.components.CustomStyle;
import com.vaadin.flow.html.Div;
import com.vaadin.ui.AngularTemplate;

public class MainLayout extends AngularTemplate {

    @Id("custom-styles")
    private Div customStylesWrapper;

    public MainLayout() {
        customStylesWrapper.getElement().appendChild(CustomStyle.getAppStyles().getElement());
    }

}
