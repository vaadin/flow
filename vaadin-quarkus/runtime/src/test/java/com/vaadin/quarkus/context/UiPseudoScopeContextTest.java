/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import io.quarkus.arc.InjectableContext;
import io.quarkus.test.junit.QuarkusTest;

import com.vaadin.quarkus.context.UiContextTest.TestUIScopedContext;
import com.vaadin.quarkus.context.UiPseudoScopeContextTest.TestUIContextWrapper;

@QuarkusTest
public class UiPseudoScopeContextTest
        extends InjectableContextTest<TestUIContextWrapper> {

    @Override
    protected UnderTestContext newContextUnderTest() {
        return new UIUnderTestContext();
    }

    @Override
    protected Class<TestUIContextWrapper> getContextType() {
        return TestUIContextWrapper.class;
    }

    public static class TestUIContextWrapper extends UIContextWrapper {

        @Override
        InjectableContext getContext() {
            return new TestUIScopedContext();
        }
    }
}
