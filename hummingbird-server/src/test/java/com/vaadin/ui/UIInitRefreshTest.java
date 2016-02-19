/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.ui;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.server.VaadinRequest;

public class UIInitRefreshTest {

    private boolean initCalled;

    private class TestUI extends UI {
        @Override
        protected void init(VaadinRequest request) {
            initCalled = true;
        }

    }

    @Before
    public void setUp() {
        initCalled = false;
    }

    @Test
    public void testListenersCalled() {
        IMocksControl control = EasyMock.createNiceControl();

        VaadinRequest initRequest = control.createMock(VaadinRequest.class);
        EasyMock.expect(initRequest.getParameter("v-loc"))
                .andReturn("http://example.com/#foo");
        EasyMock.expect(initRequest.getParameter("v-cw")).andReturn("100");
        EasyMock.expect(initRequest.getParameter("v-ch")).andReturn("100");

        VaadinRequest reinitRequest = control.createMock(VaadinRequest.class);
        EasyMock.expect(reinitRequest.getParameter("v-loc"))
                .andReturn("http://example.com/#bar");
        EasyMock.expect(reinitRequest.getParameter("v-cw")).andReturn("200");
        EasyMock.expect(reinitRequest.getParameter("v-ch")).andReturn("200");

        control.replay();

        UI ui = new TestUI();
        ui.doInit(initRequest, 0, "");

        Assert.assertTrue(initCalled);

    }
}
