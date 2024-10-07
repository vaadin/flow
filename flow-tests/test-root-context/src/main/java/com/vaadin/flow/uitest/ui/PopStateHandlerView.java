package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

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
        String historyPush = "window.history.pushState(null, null, event.target.textContent)";
        if (VaadinSession.getCurrent().getService().getDeploymentConfiguration()
                .isReactEnabled()) {
            historyPush = "window.dispatchEvent(new CustomEvent('vaadin-navigate', { detail: {  url: event.target.textContent, replace: false } }))";
        }
        button.addEventListener("click", e -> {
        }).addEventData(historyPush);
        return button;
    }
}
