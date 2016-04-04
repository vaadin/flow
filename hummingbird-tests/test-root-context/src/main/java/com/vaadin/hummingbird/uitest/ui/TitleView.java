package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.annotations.Title;
import com.vaadin.hummingbird.uitest.component.Div;

@Title("Title view")
public class TitleView extends AbstractDivView {

    @Override
    protected void onShow() {
        removeAllComponents();
        Div titleView = new Div("Title view");
        titleView.setId("annotated");
        addComponents(titleView);
    }

}
