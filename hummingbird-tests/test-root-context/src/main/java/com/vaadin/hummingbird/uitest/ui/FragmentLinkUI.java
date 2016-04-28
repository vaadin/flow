package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementFactory;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

public class FragmentLinkUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        getPage().executeJavaScript("var i = 0;"
                + "window.addEventListener('hashchange', function(event) {"
                + "debugger;" + "var x = document.createElement('span');"
                + "x.textContent = ' ' + i;" + "i++;"
                + "x.class = 'hashchange';"
                + "document.getElementById('placeholder').appendChild(x);},"
                + " false);");

        Element bodyElement = getElement();
        bodyElement.getStyle().set("margin", "1em");

        Element scrollLocator = ElementFactory.createDiv()
                .setAttribute("id", "scrollLocator")
                .setTextContent("Scroll locator");
        scrollLocator.getStyle().set("position", "fixed").set("top", "0");

        Element location = ElementFactory.createDiv("no location")
                .setAttribute("id", "location");
        Element placeholder = ElementFactory.createDiv("Hash Change Events")
                .setAttribute("id", "placeholder");

        bodyElement.appendChild(scrollLocator, location, placeholder,
                new Element("p"));

        Element scrollToLink = ElementFactory.createRouterLink(
                "/run/com.vaadin.hummingbird.uitest.ui.FragmentLinkUI#Scroll_Target",
                "Scroller link");
        Element scrollToLink2 = ElementFactory.createRouterLink(
                "/run/com.vaadin.hummingbird.uitest.ui.FragmentLinkUI#Scroll_Target2",
                "Scroller link 2");
        Element scrollToLinkAnotherView = ElementFactory.createRouterLink(
                "./foobar#Scroll_Target", "Scroller link with different view");
        Element linkThatIsOverridden = ElementFactory.createRouterLink(
                "./override#Scroll_Target", "Link that server overrides");

        Element scrollTarget = ElementFactory.createHeading1("Scroll Target")
                .setAttribute("id", "Scroll_Target");
        Element scrollTarget2 = ElementFactory.createHeading2("Scroll Target 2")
                .setAttribute("id", "Scroll_Target2");

        bodyElement.appendChild(scrollToLink, new Element("p"), scrollToLink2,
                new Element("p"), scrollToLinkAnotherView, new Element("p"),
                linkThatIsOverridden, new Element("p"), createSpacer(),
                scrollTarget, createSpacer(), scrollTarget2, createSpacer());

        getPage().getHistory().setHistoryStateChangeHandler(e -> {
            if (e.getLocation().startsWith("override")) {
                getPage().getHistory().replaceState(null,
                        "overridden#Scroll_Target2");
            } else {
                location.setTextContent(e.getLocation());
            }
        });
    }

    private Element createSpacer() {
        Element spacer = ElementFactory.createDiv().setTextContent("spacer");
        spacer.getStyle().set("height", "1000px");
        return spacer;
    }

}
