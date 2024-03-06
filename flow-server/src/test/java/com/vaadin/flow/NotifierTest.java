/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow;

import org.junit.Test;

import com.vaadin.flow.component.BlurNotifier;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.CompositionNotifier;
import com.vaadin.flow.component.FocusNotifier;
import com.vaadin.flow.component.InputNotifier;
import com.vaadin.flow.component.KeyNotifier;
import com.vaadin.flow.component.Tag;

public class NotifierTest {
    @Tag("div")
    public static class NotifierComponent extends Component
            implements BlurNotifier<NotifierComponent>,
            ClickNotifier<NotifierComponent>, CompositionNotifier,
            FocusNotifier<NotifierComponent>, InputNotifier, KeyNotifier {
    }

    @Test
    public void addNotifiers() {
        // Just testing that adding notifiers actually compiles and doesn't
        // throw. Test is on purpose outside com.vaadin.flow.component to
        // uncover visibility problems.
        NotifierComponent component = new NotifierComponent();
        component.addBlurListener(event -> ignore());
        component.addClickListener(event -> ignore());
        component.addCompositionStartListener(event -> ignore());
        component.addCompositionEndListener(event -> ignore());
        component.addCompositionUpdateListener(event -> ignore());
        component.addInputListener(event -> ignore());
        component.addKeyDownListener(event -> ignore());
        component.addKeyUpListener(event -> ignore());
        component.addKeyPressListener(event -> ignore());
    }

    private static void ignore() {
        // This method is here to avoid line breaks from {} lambas
    }
}
