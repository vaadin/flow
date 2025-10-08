/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;

public abstract class AbstractTestViewWithLog extends Div {

    private Log log = new Log();

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        /*
         * Read push settings from the UI instead of the the navigation target /
         * router layout to preserve the structure of these legacy testing UIs
         */
        CustomPush push = getClass().getAnnotation(CustomPush.class);

        if (push != null) {
            attachEvent.getUI().getPushConfiguration()
                    .setPushMode(push.value());
            attachEvent.getUI().getPushConfiguration()
                    .setTransport(push.transport());
        }

        add(log);
    }

    protected void log(String msg) {
        log.log(msg);
    }
}
