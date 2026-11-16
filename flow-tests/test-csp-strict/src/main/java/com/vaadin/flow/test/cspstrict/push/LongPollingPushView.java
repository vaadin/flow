/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.test.cspstrict.push;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

/**
 * Server push over long polling. The transport of a UI is read by the browser
 * when it opens the push connection, so this view closes the connection that
 * the application opens for every UI, and opens it again over long polling for
 * the click that starts the background task.
 */
@Route("push-long-polling")
public class LongPollingPushView extends AbstractPushView {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        attachEvent.getUI().getPushConfiguration()
                .setPushMode(PushMode.DISABLED);
    }

    @Override
    protected void beforePush(UI ui) {
        // Only what the browser does from here on counts
        PushTransportRecorder.clear(ui.getSession());
        ui.getPushConfiguration().setTransport(Transport.LONG_POLLING);
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
    }
}
