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
package com.vaadin.flow.quarkus.test.executor;

import java.util.concurrent.Executor;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import com.vaadin.quarkus.annotation.VaadinServiceEnabled;

class QuarkusVaadinServletServiceCustomExecutorTest
        extends AbstractVaadinServiceExecutorTestSupport {

    @RegisterExtension
    static final QuarkusUnitTest config = quarkusUnitTest(TestConfig.class);

    @Override
    Executor expectedExecutor() {
        return TestConfig.CUSTOM_EXECUTOR;
    }

    static class TestConfig {

        static final Executor CUSTOM_EXECUTOR = Mockito.mock(Executor.class);

        @Produces
        @Singleton
        @VaadinServiceEnabled
        @Unremovable
        Executor vaadinExecutor() {
            return CUSTOM_EXECUTOR;
        }
    }
}
