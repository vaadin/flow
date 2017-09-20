package com.vaadin.flow.uitest.ui;

import com.vaadin.ui.event.Tag;
import com.vaadin.router.Title;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.LocationChangeEvent;

@Title("foobar")
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
