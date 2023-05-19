package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.PopStateHandlerView")
public class PopStateHandlerView extends RouterLinkView {

    @Override
    protected void addLinks() {
        getElement().appendChild(
                createPushStateButtons(
                        "com.vaadin.flow.uitest.ui.PopStateHandlerUI/another/"),
                ElementFactory.createParagraph(),
                createPushStateButtons(
                        "com.vaadin.flow.uitest.ui.PopStateHandlerUI/forum/"),
                ElementFactory.createParagraph(),
                createPushStateButtons(
                        "com.vaadin.flow.uitest.ui.PopStateHandlerUI/forum/#!/category/1"),
                ElementFactory.createParagraph(),
                createPushStateButtons(
                        "com.vaadin.flow.uitest.ui.PopStateHandlerUI/forum/#!/category/2"),
                ElementFactory.createParagraph(), createPushStateButtons(
                        "com.vaadin.flow.uitest.ui.PopStateHandlerUI/forum/#"));
    }

    protected Element createPushStateButtons(String target) {
        Element button = ElementFactory.createButton(target).setAttribute("id",
                target);
        button.addEventListener("click", e -> {
        }).addEventData(
                "window.history.pushState(null, null, event.target.textContent)");
        return button;
    }
}
