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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class InitParametersTest {
    @Test
    public void publicMembersAreStringConstants() {
        for (Field field : InitParameters.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers)) {
                Assertions.assertEquals(String.class, field.getType(), String
                        .format("field '%s' expected String", field.getName()));
                Assertions.assertTrue(Modifier.isStatic(modifiers), String
                        .format("field '%s' expected static", field.getName()));
                Assertions.assertTrue(Modifier.isFinal(modifiers), String
                        .format("field '%s' expected final", field.getName()));
            }
        }
    }
}
