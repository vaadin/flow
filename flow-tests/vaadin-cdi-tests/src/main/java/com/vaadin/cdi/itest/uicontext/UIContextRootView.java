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
package com.vaadin.cdi.itest.uicontext;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import com.vaadin.cdi.annotation.CdiComponent;
import com.vaadin.cdi.itest.uicontext.UIScopedLabel.SetTextEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("")
@CdiComponent
public class UIContextRootView extends Div {

    public static final String CLOSE_UI_BTN = "CLOSE_UI_BTN";
    public static final String CLOSE_SESSION_BTN = "CLOSE_SESSION_BTN";
    public static final String TRIGGER_EVENT_BTN = "TRIGGER_EVENT_BTN";
    public static final String INJECTER_LINK = "injecter view";
    public static final String UISCOPED_LINK = "uiscoped view";
    public static final String UIID_LABEL = "UIID_LABEL";
    public static final String NORMALSCOPED_LINK = "normalscoped bean view";
    public static final String EVENT_PAYLOAD = "EVENT_PAYLOAD";

    @Inject
    private UIScopedLabel label;

    @Inject
    private Event<SetTextEvent> setTextEventTrigger;

    @PostConstruct
    private void init() {
        final String uiIdStr = UI.getCurrent().getUIId() + "";
        label.setText(uiIdStr);

        final NativeLabel uiId = new NativeLabel(uiIdStr);
        uiId.setId(UIID_LABEL);

        final NativeButton closeUI = new NativeButton("close UI",
                event -> getUI().ifPresent(UI::close));
        closeUI.setId(CLOSE_UI_BTN);

        final NativeButton closeSession = new NativeButton("close session",
                event -> getUI().ifPresent(ui -> ui.getSession().close()));
        closeSession.setId(CLOSE_SESSION_BTN);

        final NativeButton triggerEvent = new NativeButton("event trigger",
                event -> setTextEventTrigger
                        .fire(new SetTextEvent(EVENT_PAYLOAD)));
        triggerEvent.setId(TRIGGER_EVENT_BTN);

        final Div navDiv = new Div(
                new RouterLink(INJECTER_LINK, UIScopeInjecterView.class),
                new RouterLink(UISCOPED_LINK, UIScopedView.class),
                new RouterLink(NORMALSCOPED_LINK,
                        UINormalScopedBeanView.class));

        add(new Div(uiId), new Div(closeUI, closeSession),
                new Div(triggerEvent), new Div(this.label), navDiv);
    }

}
