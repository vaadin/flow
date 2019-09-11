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
package com.vaadin.flow.ccdmtest;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;

@Route(value = "view-with-all-events", layout = MainLayout.class)
public class ViewWithAllEvents extends Div implements BeforeEnterObserver,
        AfterNavigationObserver, HasUrlParameter<String>, BeforeLeaveObserver {
    private Div logger;

    public ViewWithAllEvents() {
        logger = new Div();
        add(logger);
    }

    @Override
    public void setParameter(BeforeEvent event,
            @OptionalParameter String parameter) {
        addLog("1 setParameter");
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        addLog("4 afterNavigation");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        addLog("2 beforeEnter");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        addLog("3 onAttach");
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        addLog("onDetach");
    }

    private void addLog(String log) {
        logger.add(new Paragraph("ViewWithAllEvents: " + log));
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        addLog("beforeLeave");
    }
}
