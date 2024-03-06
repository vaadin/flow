/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.util.Random;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

public abstract class AbstractLiveReloadView extends Div {
    public static final String INSTANCE_IDENTIFIER = "instance-identifier";
    public static final String ATTACH_IDENTIFIER = "attach-identifier";

    private static final Random random = new Random();

    private Span attachIdLabel = new Span();

    public AbstractLiveReloadView() {
        getStyle().set("display", "flex");
        getStyle().set("flex-direction", "column");
        getStyle().set("align-items", "flex-start");

        Span instanceIdLabel = new Span(Integer.toString(random.nextInt()));
        instanceIdLabel.setId(INSTANCE_IDENTIFIER);
        add(instanceIdLabel);

        attachIdLabel.setId(ATTACH_IDENTIFIER);
        add(attachIdLabel);
        addAttachListener(e -> {
            attachIdLabel.setText(Integer.toString(random.nextInt()));
        });
    }

}
