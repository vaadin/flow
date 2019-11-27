/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.server.VaadinRequest;

public class AddDivUI extends UI {

    private int msgId = 1;
    private String ip;

    @Override
    protected void init(VaadinRequest request) {
        ip = request.getRemoteAddr();
        addDiv();

        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        long delay = 10;
        ses.scheduleAtFixedRate(() -> {

            access(() -> {
                addDiv();
            });

            if (msgId > 500) {
                throw new RuntimeException("Done");
            }
        }, delay, delay, TimeUnit.MILLISECONDS);
    }

    private void addDiv() {
        Element bodyElement = getElement();
        Element div = ElementFactory.createDiv("Hello world at "
                + System.currentTimeMillis() + " (" + msgId++ + ")");
        bodyElement.insertChild(0, div);
        if (msgId % 100 == 0) {
            System.out.println("Pushed id " + msgId + " to " + ip);
        }
        // FIXME Enable when remove works
        // while (bodyElement.getChildCount() > 20) {
        // bodyElement.removeChild(20);
        // }
    }
}
