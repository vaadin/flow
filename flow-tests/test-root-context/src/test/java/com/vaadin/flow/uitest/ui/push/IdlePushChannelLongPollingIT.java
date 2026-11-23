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
package com.vaadin.flow.uitest.ui.push;

import org.junit.experimental.categories.Category;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.testcategory.PushTests;

@Category(PushTests.class)
public class IdlePushChannelLongPollingIT extends IdlePushChannelIT {
    @Override
    protected Class<? extends Component> getViewClass() {
        return BasicPushLongPollingView.class;
    }

    /**
     * The push client reports a long polling connection only once: when the
     * server resumes an idle request, the client just polls again. Nothing is
     * pushed while the test idles, so a new poll request of the push connection
     * can only follow the idle timeout.
     */
    @Override
    protected boolean isReconnectLog(String message) {
        return message.startsWith("[Atmosphere] Long-polling request: ")
                && message.contains("v-r=push");
    }
}
