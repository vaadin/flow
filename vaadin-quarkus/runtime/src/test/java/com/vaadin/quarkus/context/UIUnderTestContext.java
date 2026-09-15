/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.quarkus.context;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

public class UIUnderTestContext implements UnderTestContext {

    private VaadinSession session;
    private UI ui;
    private static int uiIdNdx = 0;
    private static SessionUnderTestContext sessionContextUnderTest;

    public UIUnderTestContext() {
        this(null);
    }

    protected UIUnderTestContext(VaadinSession session) {
        this.session = session;
    }

    private void mockUI() {
        if (session == null) {
            mockSession();
        }

        ui = new UI();
        ui.getInternals().setSession(session);
        uiIdNdx++;
        ui.doInit(null, uiIdNdx, "appId");
    }

    private void mockSession() {
        if (sessionContextUnderTest == null) {
            sessionContextUnderTest = new SessionUnderTestContext();
            sessionContextUnderTest.activate();
        }
        session = sessionContextUnderTest.getSession();
    }

    @Override
    public void activate() {
        if (ui == null) {
            mockUI();
        }
        UI.setCurrent(ui);
    }

    @Override
    public void tearDownAll() {
        UI.setCurrent(null);
        uiIdNdx = 0;
        if (sessionContextUnderTest != null) {
            sessionContextUnderTest.tearDownAll();
            sessionContextUnderTest = null;
        }
    }

    @Override
    public void destroy() {
        ComponentUtil.onComponentDetach(ui);
    }

    public UI getUi() {
        return ui;
    }

}
