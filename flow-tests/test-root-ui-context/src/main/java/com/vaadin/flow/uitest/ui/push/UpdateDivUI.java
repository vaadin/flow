/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.ui.Push;
import com.vaadin.ui.UI;

@Push
public class UpdateDivUI extends UI {

    private int msgId = 1;
    private Element div = ElementFactory.createDiv();

    @Override
    protected void init(VaadinRequest request) {
        Element bodyElement = getElement();
        bodyElement.appendChild(div);
        updateDiv();

        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        long delay = 2;
        ses.scheduleAtFixedRate(() -> {

            access(() -> {
                updateDiv();
            });

            if (msgId > 1000) {
                throw new RuntimeException("Done");
            }
        }, delay, delay, TimeUnit.MILLISECONDS);
    }

    private void updateDiv() {
        div.setText("Hello world at " + System.currentTimeMillis() + " ("
                + msgId++ + ")");
    }
}
