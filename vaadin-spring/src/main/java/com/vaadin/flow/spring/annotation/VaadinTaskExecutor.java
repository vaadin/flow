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
 *
 */

package com.vaadin.flow.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Qualifier annotation for marking a
 * {@link org.springframework.core.task.TaskExecutor} bean as a Vaadin-specific
 * Executor.
 * <p>
 * The bean annotated with this qualifier will function as the primary
 * {@link org.springframework.core.task.TaskExecutor} for Vaadin-specific
 * asynchronous operations.
 * <p>
 * Alternatively, a {@link org.springframework.core.task.TaskExecutor} bean can
 * be declared with the name {@code VaadinTaskExecutor}.
 * <p>
 * Constraints:
 * <ul>
 * <li>There should be at most one
 * {@link org.springframework.core.task.TaskExecutor} bean annotated with
 * {@code @VaadinTaskExecutor}.</li>
 * <li>Both an annotated bean and a bean with the name
 * {@code VaadinTaskExecutor} cannot exist simultaneously.</li>
 * </ul>
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER,
        ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Qualifier
public @interface VaadinTaskExecutor {
    /**
     * The name of the {@link org.springframework.core.task.TaskExecutor} bean
     * specifically used for Vaadin-related asynchronous tasks. This constant
     * can be used as a bean name to designate a single
     * {@link org.springframework.core.task.TaskExecutor} implementation as the
     * primary executor for handling Vaadin-specific task execution.
     */
    String NAME = "VaadinTaskExecutor";
}
