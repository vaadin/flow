/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.UI;

public class ReconnectLongPollingIT extends ReconnectTest {

    @Override
    protected Class<? extends UI> getUIClass() {
        return BasicPushLongPollingUI.class;
    }

}
