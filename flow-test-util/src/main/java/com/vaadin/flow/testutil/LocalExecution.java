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
package com.vaadin.flow.testutil;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.testbench.annotations.RunLocally;
import com.vaadin.testbench.parallel.Browser;

/**
 * Allows to configure tests for local execution.
 * <p>
 * Similar to {@link RunLocally} but {@link LocalExecution} is enabled by
 * default for any test which extends {@link AbstractTestBenchTest} and can be
 * overridden by {@link AbstractTestBenchTest#USE_HUB_PROPERTY}. If this
 * property value is set to {@code true} then the test will be executed on the
 * tests Hub.
 *
 * @see RunLocally
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface LocalExecution {

    /**
     * Gets the browser for local execution.
     *
     * @return the browser for local execution
     */
    Browser value() default Browser.CHROME;

    /**
     * Gets the browser version.
     *
     * @return the browser version
     */
    String browserVersion() default "";

    /**
     * Checks whether the local execution configuration active.
     * <p>
     * If configuration is not active then the test will be executed on the
     * tests Hub.
     *
     * @return whether the local execution configuration is active
     */
    boolean active() default true;
}
