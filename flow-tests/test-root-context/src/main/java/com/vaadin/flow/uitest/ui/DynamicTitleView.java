package com.vaadin.flow.uitest.ui;

import com.vaadin.router.PageTitle;
import com.vaadin.ui.Tag;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.LocationChangeEvent;

@PageTitle("foobar")
public class DynamicTitleView extends AbstractDivView {

    @Override
    public String getTitle(LocationChangeEvent locationChangeEvent) {
        return "dynamic title view";
    }

    @Override
    protected void onShow() {
        getElement().removeAllChildren().appendChild(new Element(Tag.DIV)
                .setText("Dynamic").setAttribute("id", "dynamic"));
    }

}
