/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;

public class InitParametersTest {
    @Test
    public void publicMembersAreStringConstants() {
        for (Field field : InitParameters.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers)) {
                Assert.assertEquals(
                        String.format("field '%s' expected String", field.getName()),
                        String.class, field.getType());
                Assert.assertTrue(
                        String.format("field '%s' expected static", field.getName()),
                        Modifier.isStatic(modifiers));
                Assert.assertTrue(
                        String.format("field '%s' expected final", field.getName()),
                        Modifier.isFinal(modifiers));
            }
        }
    }
}
