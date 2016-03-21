package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.annotations.Title;
import com.vaadin.hummingbird.dom.Element;

@Title("Title view")
public class TitleView extends TestView {

    @Override
    protected void onShow() {
        getElement().removeAllChildren().appendChild(new Element("div")
                .setTextContent("Title view").setAttribute("id", "annotated"));
    }

}
