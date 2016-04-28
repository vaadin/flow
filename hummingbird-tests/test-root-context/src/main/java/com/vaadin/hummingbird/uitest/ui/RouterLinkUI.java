package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementFactory;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

public class RouterLinkUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        Element bodyElement = getElement();
        bodyElement.getStyle().set("margin", "1em");

        Element location = ElementFactory.createDiv("no location")
                .setAttribute("id", "location");

        bodyElement.appendChild(location, new Element("p"));

        addLinks();

        getPage().getHistory().setHistoryStateChangeHandler(e -> {
            location.setTextContent(e.getLocation());
        });
    }

    protected void addLinks() {
        getElement().appendChild(
                // inside servlet mapping
                ElementFactory.createDiv("inside this servlet"),
                ElementFactory.createRouterLink("", "empty"), new Element("p"),
                createRouterLink("foo"), new Element("p"),
                createRouterLink("foo/bar"), new Element("p"),
                createRouterLink("./foobar"), new Element("p"),
                createRouterLink("./foobar?what=not"), new Element("p"),
                createRouterLink("./foobar?what=not#fragment"),
                new Element("p"), createRouterLink("/run/baz"),
                new Element("p"),
                // outside
                ElementFactory.createDiv("outside this servlet"),
                createRouterLink("/run"), new Element("p"),
                createRouterLink("/foo/bar"), new Element("p"),
                // external
                ElementFactory.createDiv("external"),
                createRouterLink("http://google.com"));
    }

    private Element createRouterLink(String target) {
        return ElementFactory.createRouterLink(target, target);
    }

}
