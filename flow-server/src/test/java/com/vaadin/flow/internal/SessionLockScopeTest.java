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
package com.vaadin.flow.internal;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class SessionLockScopeTest {

    @AfterEach
    void clearCurrentInstances() {
        CurrentInstance.clearAll();
    }

    @Test
    void close_unlockThrows_previousInstancesRestored() {
        CurrentInstance.set(SessionLockScopeTest.class, this);
        VaadinSession session = mock(VaadinSession.class);
        IllegalStateException failure = new IllegalStateException();
        doThrow(failure).when(session).unlock();
        SessionLockScope lock = SessionLockScope.lock(session, mock(UI.class));

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                lock::close);

        assertSame(failure, thrown);
        assertNull(UI.getCurrent());
        assertSame(this, CurrentInstance.get(SessionLockScopeTest.class));
    }

    @Test
    void close_unlockThrowsWithFailureHandler_handlerGetsFailureAndPreviousInstancesRestored() {
        CurrentInstance.set(SessionLockScopeTest.class, this);
        VaadinSession session = mock(VaadinSession.class);
        IllegalStateException failure = new IllegalStateException();
        doThrow(failure).when(session).unlock();
        List<RuntimeException> handled = new ArrayList<>();
        SessionLockScope lock = SessionLockScope.lock(session, mock(UI.class))
                .onUnlockFailure(handled::add);

        lock.close();

        assertEquals(List.of(failure), handled);
        assertNull(UI.getCurrent());
        assertSame(this, CurrentInstance.get(SessionLockScopeTest.class));
    }
}
