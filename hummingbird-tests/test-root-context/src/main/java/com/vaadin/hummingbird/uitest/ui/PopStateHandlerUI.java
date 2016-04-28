package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementFactory;

public class PopStateHandlerUI extends RouterLinkUI {

    @Override
    protected void addLinks() {
        getElement().appendChild(
                createPushStateButtons(
                        "com.vaadin.hummingbird.uitest.ui.PopStateHandlerUI/another/"),
                ElementFactory.createParagraph(),
                createPushStateButtons(
                        "com.vaadin.hummingbird.uitest.ui.PopStateHandlerUI/forum/"),
                ElementFactory.createParagraph(),
                createPushStateButtons(
                        "com.vaadin.hummingbird.uitest.ui.PopStateHandlerUI/forum/#!/category/1"),
                ElementFactory.createParagraph(),
                createPushStateButtons(
                        "com.vaadin.hummingbird.uitest.ui.PopStateHandlerUI/forum/#!/category/2"),
                ElementFactory.createParagraph(), createPushStateButtons(
                        "com.vaadin.hummingbird.uitest.ui.PopStateHandlerUI/forum/#"));
    }

    protected Element createPushStateButtons(String target) {
        Element button = ElementFactory.createButton(target).setAttribute("id",
                target);
        button.addEventListener("click", e -> {
        }, "window.history.pushState(null, null, event.target.textContent)");
        return button;
    }
}
