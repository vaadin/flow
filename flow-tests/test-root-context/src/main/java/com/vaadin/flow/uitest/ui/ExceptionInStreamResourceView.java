/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

@Route("com.vaadin.flow.uitest.ui.ExceptionInStreamResourceView")
public class ExceptionInStreamResourceView extends Div {

    public ExceptionInStreamResourceView() {
        StreamResource faulty = new StreamResource(
                "you-should-not-see-this-download.pdf", () -> {
                    throw new IllegalStateException(
                            "Oops we cannot generate the stream");
                });
        Anchor anchor = new Anchor(faulty, "Click Here");
        anchor.getElement().setAttribute("download", true);
        anchor.setId("link");

        add(anchor);
    }
}
