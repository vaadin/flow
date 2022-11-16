package com.vaadin.flow.uitest.ui.push;

import org.junit.jupiter.api.Tag;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.testutil.TestTag;

@Tag(TestTag.PUSH_TESTS)
public class IdlePushChannelWebsocketIT extends IdlePushChannelIT {

    @Override
    protected Class<? extends UI> getUIClass() {
        return BasicPushWebsocketUI.class;
    }
}
