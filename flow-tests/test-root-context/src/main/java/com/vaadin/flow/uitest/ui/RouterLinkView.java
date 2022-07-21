package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.Route;

import elemental.json.JsonObject;

@Route("com.vaadin.flow.uitest.ui.RouterLinkView")
public class RouterLinkView extends AbstractDivView {

    public RouterLinkView() {
        Element bodyElement = getElement();
        bodyElement.getStyle().set("margin", "1em");

        Element location = ElementFactory.createDiv("no location")
                .setAttribute("id", "location");

        Element queryParams = ElementFactory.createDiv("no queryParams")
                .setAttribute("id", "queryParams");

        bodyElement.appendChild(location)
                .appendChild(new Element("p"));
        bodyElement.appendChild(queryParams)
                .appendChild(new Element("p"));

        addLinks();

        getPage().getHistory().setHistoryStateChangeHandler(e -> {
            location.setText(e.getLocation().getPath());
            queryParams.setText(
                    e.getLocation().getQueryParameters().getQueryString());
            if (e.getState().isPresent())
                UI.getCurrent().getPage().getHistory().pushState(null,
                        ((JsonObject) e.getState().get()).getString("href"));
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
        getElement()
                // inside servlet mapping
                .appendChild(ElementFactory.createDiv("inside this servlet"))
                .appendChild(ElementFactory.createRouterLink("", "empty"))
                .appendChild(new Element("p"))
                .appendChild(createRouterLink("foo"))
                .appendChild(new Element("p"))
                .appendChild(createRouterLink("foo/bar"))
                .appendChild(new Element("p"))
                .appendChild(createRouterLink("./foobar"))
                .appendChild(new Element("p"))
                .appendChild(createRouterLink("./foobar?what=not"))
                .appendChild(new Element("p"))
                .appendChild(createRouterLink("./foobar?what=not#fragment"))
                .appendChild(new Element("p"))
                .appendChild(createRouterLink("/view/baz"))
                .appendChild(new Element("p"))
                // outside
                .appendChild(ElementFactory.createDiv("outside this servlet"))
                .appendChild(createRouterLink("/run"))
                .appendChild(new Element("p"))
                .appendChild(createRouterLink("/foo/bar"))
                .appendChild(new Element("p"))
                // external
                .appendChild(ElementFactory.createDiv("external"))
                .appendChild(createRouterLink("http://example.net/"));
    }

    private Element createRouterLink(String target) {
        return ElementFactory.createRouterLink(target, target);
    }

}
