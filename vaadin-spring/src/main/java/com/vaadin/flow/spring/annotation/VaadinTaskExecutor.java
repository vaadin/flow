/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
