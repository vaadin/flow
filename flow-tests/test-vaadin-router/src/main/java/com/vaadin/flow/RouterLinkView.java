/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.ui.AbstractDivView;

import tools.jackson.databind.node.ObjectNode;

@Route("com.vaadin.flow.RouterLinkView")
public class RouterLinkView extends AbstractDivView {

    public RouterLinkView() {
        Element bodyElement = getElement();
        bodyElement.getStyle().set("margin", "1em");

        Element location = ElementFactory.createDiv("no location")
                .setAttribute("id", "location");

        Element queryParams = ElementFactory.createDiv("no queryParams")
                .setAttribute("id", "queryParams");

        bodyElement.appendChild(location, new Element("p"));
        bodyElement.appendChild(queryParams, new Element("p"));

        addLinks();

        getPage().getHistory().setHistoryStateChangeHandler(e -> {
            location.setText(e.getLocation().getPath());
            queryParams.setText(
                    e.getLocation().getQueryParameters().getQueryString());
            if (e.getState().isPresent()) {
                ObjectNode state = ((ObjectNode) e.getState().get());
                if (state.has("href")) {
                    UI.getCurrent().getPage().getHistory().pushState(null,
                            state.get("href").asText());
                }

            }
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
                ElementFactory.createDiv("inside this servlet"),
                ElementFactory.createRouterLink("", "empty"), new Element("p"),
                createRouterLink("foo"), new Element("p"),
                createRouterLink("foo/bar"), new Element("p"),
                createRouterLink("./foobar"), new Element("p"),
                createRouterLink("./foobar?what=not"), new Element("p"),
                createRouterLink("./foobar?what=not#fragment"),
                new Element("p"), createRouterLink("/view/baz"),
                new Element("p"),
                // outside
                ElementFactory.createDiv("outside this servlet"),
                createRouterLink("/run"), new Element("p"),
                createRouterLink("/foo/bar"), new Element("p"),
                // external
                ElementFactory.createDiv("external"),
                createRouterLink("https://example.net/"));
    }

    private Element createRouterLink(String target) {
        return ElementFactory.createRouterLink(target, target);
    }

}
