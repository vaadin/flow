/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.signals.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.vaadin.signals.Id;
import com.vaadin.signals.impl.CommandResult.Accept;
import com.vaadin.signals.impl.CommandResult.NodeModification;
import com.vaadin.signals.impl.CommandResult.Reject;

public class CommandResultTest {

    @Test
    void rejectAll() {
        Map<Id, CommandResult> in = Map.of(new Id(1), CommandResult.ok(),
                new Id(2), CommandResult.fail("Original"));

        Map<Id, CommandResult> out = CommandResult.rejectAll(in, "New");

        assertEquals(Map.of(new Id(1), CommandResult.fail("New"), new Id(2),
                CommandResult.fail("Original")), out);
    }

    @Test
    void conditional_true_accepted() {
        CommandResult result = CommandResult.conditional(true, "ignored");

        assertTrue(result.accepted());
    }

    @Test
    void conditional_false_rejected() {
        CommandResult result = CommandResult.conditional(false, "reason");

        assertFalse(result.accepted());
        assertEquals("reason", ((Reject) result).reason());
    }

    @Test
    void onlyUpdate_singleChange_changeReturned() {
        NodeModification modification = new NodeModification(null, null);

        Accept result = new Accept(Map.of(Id.random(), modification), Map.of());

        NodeModification update = result.onlyUpdate();
        assertSame(modification, update);
    }

    @Test
    void onlyUpdate_noChanges_throws() {
        Accept result = new Accept(Map.of(), Map.of());

        assertThrows(AssertionError.class, () -> {
            result.onlyUpdate();
        });
    }

    @Test
    void onlyUpdate_multipleChanges_throws() {
        NodeModification modification = new NodeModification(null, null);

        Accept result = new Accept(
                Map.of(Id.random(), modification, Id.random(), modification),
                Map.of());

        assertThrows(AssertionError.class, () -> {
            result.onlyUpdate();
        });
    }
}
