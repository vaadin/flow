/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.testnpmonlyfeatures.bytecodescanning;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.testnpmonlyfeatures.bytecodescanning.EagerViewWithLazyComponent", layout = ViewTestLayout.class)
public class EagerViewWithLazyComponent extends Div {

    public EagerViewWithLazyComponent() {
        add(new Paragraph(
                "Below this, you should see 'Lazy component' if the LazyView has been visited, otherwise it is empty"));
        getElement().appendChild(new Element("lazy-component"));
    }
}
