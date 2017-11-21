/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import elemental.json.Json;
import elemental.json.JsonObject;

public class ConstantPoolTest {
    private ConstantPool constantPool = new ConstantPool();

    @Test
    public void newConstantPool_noNewItems() {
        assertFalse(constantPool.hasNewConstants());
        assertEquals(0, constantPool.dumpConstants().keys().length);
    }

    @Test
    public void valueIsRegistered() {
        ConstantPoolKey reference = new ConstantPoolKey(Json.createObject());

        String constantId = constantPool.getConstantId(reference);

        assertTrue(constantPool.hasNewConstants());

        JsonObject dump = constantPool.dumpConstants();

        assertEquals(1, dump.keys().length);
        assertEquals("{}", dump.get(constantId).toJson());
    }

    @Test
    public void sameValue_sameId() {
        ConstantPoolKey reference = new ConstantPoolKey(Json.createObject());

        String constantId = constantPool.getConstantId(reference);
        constantPool.dumpConstants();

        String otherId = constantPool
                .getConstantId(new ConstantPoolKey(Json.createObject()));

        assertEquals(constantId, otherId);
        assertFalse(constantPool.hasNewConstants());
    }

    @Test
    public void differentValue_differentId() {
        ConstantPoolKey reference = new ConstantPoolKey(Json.createObject());

        String constantId = constantPool.getConstantId(reference);
        constantPool.dumpConstants();

        String otherId = constantPool
                .getConstantId(new ConstantPoolKey(Json.createArray()));

        assertNotEquals(constantId, otherId);
        assertTrue(constantPool.hasNewConstants());
    }
}
