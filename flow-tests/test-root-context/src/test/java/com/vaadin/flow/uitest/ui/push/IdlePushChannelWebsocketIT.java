/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.push;

import org.junit.experimental.categories.Category;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testcategory.PushTests;

@Category({ PushTests.class, IgnoreOSGi.class })
public class IdlePushChannelWebsocketIT extends IdlePushChannelIT {

    @Override
    protected Class<? extends Component> getViewClass() {
        return BasicPushWebsocketView.class;
    }
}
