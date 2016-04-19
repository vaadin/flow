package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.annotations.Tag;
import com.vaadin.annotations.Title;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.router.LocationChangeEvent;

@Title("foobar")
public class DynamicTitleView extends AbstractDivView {

    @Override
    public String getTitle(LocationChangeEvent locationChangeEvent) {
        return "dynamic title view";
    }

    @Override
    protected void onShow() {
        getElement().removeAllChildren().appendChild(new Element(Tag.DIV)
                .setTextContent("Dynamic").setAttribute("id", "dynamic"));
    }

}
