/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.push;

import org.junit.experimental.categories.Category;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.testcategory.PushTests;

@Category(PushTests.class)
public class IdlePushChannelWebsocketIT extends IdlePushChannelIT {

    @Override
    protected Class<? extends UI> getUIClass() {
        return BasicPushWebsocketUI.class;
    }
}
