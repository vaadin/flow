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

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.vaadin.signals.Id;

public class OperationResultTest {

    @Test
    void rejectAll() {
        Map<Id, OperationResult> in = Map.of(new Id(1), OperationResult.ok(),
                new Id(2), OperationResult.fail("Original"));

        Map<Id, OperationResult> out = OperationResult.rejectAll(in, "New");

        assertEquals(Map.of(new Id(1), OperationResult.fail("New"), new Id(2),
                OperationResult.fail("Original")), out);
    }
}
