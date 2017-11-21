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
package com.vaadin.flow.router;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;

import org.junit.Test;

public class ImmutableRouterConfigurationTest {

    @Test
    public void modifiableRouterConfigurationGettersAvailable()
            throws Exception {
        for (Method m : RouterConfiguration.class.getMethods()) {
            if (exclude(m)) {
                continue;
            }
            assertNotNull(ImmutableRouterConfiguration.class
                    .getMethod(m.getName(), m.getParameterTypes()));
        }
    }

    private boolean exclude(Method m) {
        if (m.getDeclaringClass() == Object.class) {
            return true;
        }
        if (m.getName().startsWith("set")) {
            return true;
        }

        if (m.getName().startsWith("remove")) {
            return true;
        }

        return false;
    }
}
