package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.annotations.Title;
import com.vaadin.hummingbird.html.Div;

@Title("Title view")
public class TitleView extends AbstractDivView {

    @Override
    protected void onShow() {
        removeAll();
        Div titleView = new Div();
        titleView.setText("Title view");
        titleView.setId("annotated");
        add(titleView);
    }

}
