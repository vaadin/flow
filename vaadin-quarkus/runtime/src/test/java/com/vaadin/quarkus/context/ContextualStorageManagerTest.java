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
package com.vaadin.quarkus.context;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The storage manager keys contextual storages, and route scoped beans survive
 * a browser refresh only because the key can be changed after the fact: the
 * window name is not known until the client reports it, so the storage is first
 * keyed on the UI id and relocated afterwards.
 */
class ContextualStorageManagerTest {

    private static class TestManager
            extends AbstractContextualStorageManager<String> {
        TestManager() {
            super(false);
        }
    }

    private TestManager manager;

    @BeforeEach
    void setUp() {
        manager = new TestManager();
    }

    @Test
    void getContextualStorage_createIfNotExist_isStoredUnderTheKey() {
        ContextualStorage storage = manager.getContextualStorage("a", true);

        assertNotNull(storage);
        assertSame(storage, manager.getContextualStorage("a", false));
        assertEquals(Set.of("a"), manager.getKeySet());
    }

    @Test
    void getContextualStorage_lookupOnly_doesNotCreate() {
        assertNull(manager.getContextualStorage("a", false));
        assertTrue(manager.getKeySet().isEmpty());
    }

    @Test
    void relocate_existingKey_keepsTheSameStorageUnderTheNewKey() {
        ContextualStorage storage = manager.getContextualStorage("uid-1", true);

        manager.relocate("uid-1", "win-abc");

        assertEquals(Set.of("win-abc"), manager.getKeySet());
        assertSame(storage, manager.getContextualStorage("win-abc", false),
                "the beans have to survive the rekeying, that is the point");
    }

    @Test
    void relocate_unknownKey_doesNothing() {
        manager.getContextualStorage("a", true);

        manager.relocate("nothing-here", "win-abc");

        assertEquals(Set.of("a"), manager.getKeySet(),
                "an absent storage must not be resurrected as an empty one "
                        + "under the new key");
    }

    @Test
    void destroy_unknownKey_doesNothing() {
        manager.getContextualStorage("a", true);

        manager.destroy("nothing-here");

        assertEquals(Set.of("a"), manager.getKeySet());
    }

    @Test
    void destroy_existingKey_forgetsIt() {
        manager.getContextualStorage("a", true);

        manager.destroy("a");

        assertTrue(manager.getKeySet().isEmpty());
    }

    @Test
    void destroyAll_forgetsEverything() {
        manager.getContextualStorage("a", true);
        manager.getContextualStorage("b", true);

        manager.destroyAll();

        assertTrue(manager.getKeySet().isEmpty());
    }
}
