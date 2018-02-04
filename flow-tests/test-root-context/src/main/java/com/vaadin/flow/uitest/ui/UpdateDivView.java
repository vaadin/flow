/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.Route;

@Push
@Route("com.vaadin.flow.uitest.ui.UpdateDivView")
public class UpdateDivView extends AbstractDivView {
    private int msgId = 1;
    private Element div = ElementFactory.createDiv();

    private ScheduledExecutorService ses;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        Element bodyElement = getElement();
        bodyElement.appendChild(div);
        updateDiv();

        ses = Executors.newScheduledThreadPool(1);
        long delay = 2;
        ses.scheduleAtFixedRate(() -> {

            attachEvent.getUI().access(() -> {
                updateDiv();
            });

            if (msgId > 1000) {
                throw new RuntimeException("Done");
            }
        }, delay, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);

        if (ses != null) {
            ses.shutdownNow();
            ses = null;
        }
    }

    private void updateDiv() {
        div.setText("Hello world at " + System.currentTimeMillis() + " ("
                + msgId++ + ")");
    }

}
