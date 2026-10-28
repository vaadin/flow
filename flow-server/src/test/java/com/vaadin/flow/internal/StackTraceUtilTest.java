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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StackTraceUtilTest {

    @ParameterizedTest
    @CsvSource({ "com.example.MyView, true",
            "com.vaadin.flow.router.Router, true",
            "io.quarkus.runtime.Application, true",
            "com.vaadin.flow.dom.Element, false",
            "org.jboss.weld.bean.proxy.ProxyMethodHandler, false",
            "com.vaadin.quarkus.QuarkusInstantiator, false",
            "io.quarkus.arc.impl.ClientProxies, false" })
    void isApplicationClass(String className, boolean applicationClass) {
        assertEquals(applicationClass,
                StackTraceUtil.isApplicationClass(className));
    }
}
