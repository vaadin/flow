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
package com.vaadin.flow.dom;

import java.util.LinkedList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.tests.util.MockUI;

import static org.junit.Assert.assertTrue;

/**
 * Base class for unit testing Signals. Mocks VaadinService, VaadinSession and
 * UI before each test. VaadinSession's error handler is customized to add any
 * errors to {@link #events} list (including errors caught in Signal effects).
 */
public abstract class SignalsUnitTest {

    private static MockVaadinServletService service;

    protected LinkedList<ErrorEvent> events;

    private MockUI ui;

    @BeforeClass
    @BeforeAll
    public static void init() {
        service = new MockVaadinServletService();
    }

    @AfterClass
    @AfterAll
    public static void clean() {
        CurrentInstance.clearAll();
        service.destroy();
    }

    @Before
    @BeforeEach
    public void before() {
        events = mockLockedSessionWithErrorHandler();
    }

    @After
    @AfterEach
    public void after() {
        assertTrue(events.isEmpty());
        CurrentInstance.clearAll();
        events = null;
        ui = null;
    }

    private LinkedList<ErrorEvent> mockLockedSessionWithErrorHandler() {
        VaadinService.setCurrent(service);

        var session = new MockVaadinSession(service);
        session.lock();

        // UI is set to field to avoid too eager GC due to WeakReference in
        // CurrentInstance.
        ui = new MockUI(session);
        var events = new LinkedList<ErrorEvent>();
        session.setErrorHandler(events::add);

        return events;
    }
}
