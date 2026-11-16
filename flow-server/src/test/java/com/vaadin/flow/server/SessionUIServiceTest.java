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
package com.vaadin.flow.server;

import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionUIServiceTest {

    private final SessionUIService service = new SessionUIService();
    private final VaadinSession session = mock(VaadinSession.class);
    private final VaadinSession otherSession = mock(VaadinSession.class);

    @Test
    void addUI_uisAreStoredPerSession() {
        UI ui0 = ui(0);
        UI ui1 = ui(1);
        UI other = ui(0);

        service.addUI(session, ui0);
        service.addUI(session, ui1);
        service.addUI(otherSession, other);

        assertEquals(List.of(ui0, ui1), service.getUIs(session));
        assertEquals(List.of(other), service.getUIs(otherSession));
        assertSame(ui1, service.getUIById(session, 1));
        assertNull(service.getUIById(session, 2));
    }

    @Test
    void addUI_sameId_replacesUI() {
        UI first = ui(0);
        UI second = ui(0);

        service.addUI(session, first);
        service.addUI(session, second);

        assertEquals(List.of(second), service.getUIs(session));
    }

    @Test
    void removeUI_uiIsRemoved() {
        UI ui = ui(0);
        service.addUI(session, ui);

        assertTrue(service.removeUI(session, ui));
        assertFalse(service.removeUI(session, ui));
        assertTrue(service.getUIs(session).isEmpty());
    }

    @Test
    void removeSession_allUIsOfSessionAreDropped() {
        UI ui = ui(0);
        service.addUI(session, ui);
        service.addUI(otherSession, ui(0));

        assertEquals(List.of(ui), service.removeSession(session));
        assertTrue(service.getUIs(session).isEmpty());
        assertFalse(service.getSessions().contains(session));
        assertTrue(service.getSessions().contains(otherSession));
    }

    @Test
    void getUIs_unknownSession_emptyAndUnmodifiable() {
        List<UI> uis = service.getUIs(session);

        assertTrue(uis.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> uis.add(ui(0)));
    }

    @Test
    void getUIs_iterationIsSnapshot_concurrentModificationAllowed() {
        service.addUI(session, ui(0));
        service.addUI(session, ui(1));

        Iterator<UI> iterator = service.getUIs(session).iterator();
        service.addUI(session, ui(2));

        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        assertEquals(2, count);
        assertEquals(3, service.getUIs(session).size());
    }

    @Test
    void getSessionUIService_sameInstanceForService() {
        VaadinService vaadinService = new MockVaadinServletService();

        assertSame(vaadinService.getSessionUIService(),
                vaadinService.getSessionUIService());
    }

    private static UI ui(int id) {
        UI ui = mock(UI.class);
        when(ui.getUIId()).thenReturn(id);
        return ui;
    }
}
