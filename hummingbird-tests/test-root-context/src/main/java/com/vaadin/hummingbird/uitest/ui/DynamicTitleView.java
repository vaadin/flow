package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.annotations.Title;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.router.LocationChangeEvent;

@Title("foobar")
public class DynamicTitleView extends TestView {

    @Override
    public String getTitle(LocationChangeEvent locationChangeEvent) {
        return "dynamic title view";
    }

    @Override
    protected void onShow() {
        getElement().removeAllChildren().appendChild(new Element("div")
                .setTextContent("Dynamic").setAttribute("id", "dynamic"));
    }

}
