/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.testnpmonlyfeatures.general;

import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.UIInitListener;

public class TestingUiUnitListener implements UIInitListener {

    @Override
    public void uiInit(UIInitEvent event) {
        event.getUI().add(new ComponentAddedViaInitListenerView());
    }

}
