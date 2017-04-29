package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.ui.UI;

public class RouterLinkUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        Element bodyElement = getElement();
        bodyElement.getStyle().set("margin", "1em");

        Element location = new Element("div").setAttribute("id", "location")
                .setTextContent("no location");

        bodyElement.appendChild(location, new Element("p"));
        bodyElement.appendChild(
                // inside servlet mapping
                new Element("div").setTextContent("inside this servlet"),
                new Element("a").setAttribute("href", "")
                        .setAttribute(
                                ApplicationConstants.ROUTER_LINK_ATTRIBUTE, "")
                        .setTextContent("empty"),
                new Element("p"), createRoutingLink("foo"), new Element("p"),
                createRoutingLink("foo/bar"), new Element("p"),
                createRoutingLink("./foobar"), new Element("p"),
                createRoutingLink("./foobar?what=not"), new Element("p"),
                createRoutingLink("./foobar?what=not#fragment"),
                new Element("p"), createRoutingLink("/run/baz"),
                new Element("p"),
                // outside
                new Element("div").setTextContent("outside this servlet"),
                createRoutingLink("/run"), new Element("p"),
                createRoutingLink("/foo/bar"), new Element("p"),
                // external
                new Element("div").setTextContent("external"),
                createRoutingLink("http://google.com"));

        getPage().getHistory().setLocationChangeHandler(
                e -> location.setTextContent(e.getLocation()));
    }

    private Element createRoutingLink(String target) {
        return new Element("a")
                .setAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE, "")
                .setTextContent(target).setAttribute("href", target);
    }

}
