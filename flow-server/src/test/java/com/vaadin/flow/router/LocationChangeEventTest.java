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
package com.vaadin.flow.router;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.HttpStatusCode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class LocationChangeEventTest {

    private Logger logger;

    private MockedStatic<LoggerFactory> loggerFactory;

    private LocationChangeEvent event;

    @BeforeEach
    void setUp() {
        event = new LocationChangeEvent(mock(Router.class), mock(UI.class),
                NavigationTrigger.UI_NAVIGATE, new Location("secret"),
                Collections.emptyList());

        logger = spy(LoggerFactory.getLogger(LocationChangeEvent.class));
        loggerFactory = mockStatic(LoggerFactory.class);
        loggerFactory
                .when(() -> LoggerFactory.getLogger(LocationChangeEvent.class))
                .thenReturn(logger);
    }

    @AfterEach
    void tearDown() {
        loggerFactory.close();
    }

    @Test
    void setStatusCode_navigationNotCommitted_statusCodeSetAndNotLogged() {
        event.setStatusCode(HttpStatusCode.FORBIDDEN.getCode());

        assertEquals(HttpStatusCode.FORBIDDEN.getCode(), event.getStatusCode());
        verifyNoInteractions(logger);
    }

    @Test
    void setStatusCode_afterNavigationEventFired_warnsThatValueIsIgnored() {
        new AfterNavigationEvent(event);

        event.setStatusCode(HttpStatusCode.FORBIDDEN.getCode());

        verify(logger).warn(
                contains("Ignoring setStatusCode({}) for location '{}'"),
                eq(HttpStatusCode.FORBIDDEN.getCode()), eq("secret"),
                eq(HttpStatusCode.OK.getCode()));
    }

    @Test
    void rerouteTo_warnsThatRerouteIsIgnored() {
        NavigationHandler target = mock(NavigationHandler.class);

        event.rerouteTo(target);

        verify(logger).warn(contains("Ignoring rerouteTo({})"),
                eq(target.getClass().getName()), eq("secret"));
    }

    @Test
    void rerouteTo_clearedWithNull_notLogged() {
        event.rerouteTo((NavigationHandler) null);

        verifyNoInteractions(logger);
    }
}
