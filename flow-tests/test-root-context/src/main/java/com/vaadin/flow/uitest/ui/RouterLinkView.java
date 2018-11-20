package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.RouterLinkView")
public class RouterLinkView extends AbstractDivView {

    public RouterLinkView() {
        Element bodyElement = getElement();
        bodyElement.getStyle().set("margin", "1em");

        Element location = ElementFactory.createDiv("no location").setAttribute("id", "location");

        Element queryParams = ElementFactory.createDiv("no queryParams").setAttribute("id", "queryParams");

        bodyElement.appendChild(location, new Element("p"));
        bodyElement.appendChild(queryParams, new Element("p"));

        addLinks();

        getPage().getHistory().setHistoryStateChangeHandler(e -> {
            location.setText(e.getLocation().getPath());
            queryParams.setText(e.getLocation().getQueryParameters().getQueryString());
        });

        addImageLink();
    }

    private void addImageLink() {
        Anchor anchor = new Anchor("image/link", (String) null);
        anchor.getElement().setAttribute("router-link", true);
        anchor.getStyle().set("display", "block");

        Image image = new Image("", "IMAGE");
        image.setWidth("200px");
        image.setHeight("200px");

        anchor.add(image);
        add(anchor);
    }

    protected void addLinks() {
        getElement().appendChild(
                // inside servlet mapping
                ElementFactory.createDiv("inside this servlet"), ElementFactory.createRouterLink("", "empty"),
                new Element("p"), createRouterLink("foo"), new Element("p"), createRouterLink("foo/bar"),
                new Element("p"), createRouterLink("./foobar"), new Element("p"), createRouterLink("./foobar?what=not"),
                new Element("p"), createRouterLink("./foobar?what=not#fragment"), new Element("p"),
                createRouterLink("/view/baz"), new Element("p"),
                // outside
                ElementFactory.createDiv("outside this servlet"), createRouterLink("/run"), new Element("p"),
                createRouterLink("/foo/bar"), new Element("p"),
                // external
                ElementFactory.createDiv("external"), createRouterLink("http://example.net/"));
    }

    private Element createRouterLink(String target) {
        return ElementFactory.createRouterLink(target, target);
    }

}
