/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.Transport;

/**
 * Test for reproducing the bug https://github.com/vaadin/flow/issues/4353
 *
 * @since 1.0
 */
@Route("com.vaadin.flow.uitest.ui.LongPollingPushView")
@Push(transport = Transport.LONG_POLLING)
public class LongPollingPushView extends AbstractDivView {

    public LongPollingPushView() {
        Div parent = new Div();
        Span child = new Span("Some text");
        child.setId("child");
        parent.add(child);
        add(parent);
        parent.setVisible(false);

        add(createButton("Toggle visibility", "visibility",
                e -> parent.setVisible(!parent.isVisible())));
    }
}
