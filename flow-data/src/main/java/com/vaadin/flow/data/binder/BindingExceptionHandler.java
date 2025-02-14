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
package com.vaadin.flow.data.binder;

import java.io.Serializable;
import java.util.Optional;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.binder.Binder.Binding;

/**
 * Handles an {@link Exception} which may be thrown inside {@link Binding} logic
 * to be able to identify the originator of the exception (the original
 * {@link Exception} instance usually doesn't contain any information which
 * {@link HasValue} object is the source of the exception).
 *
 * @author Vaadin Ltd
 * @since
 *
 * @see BindingException
 */
@FunctionalInterface
public interface BindingExceptionHandler extends Serializable {

    /**
     * Produces a {@link BindingException} instance based on original
     * {@code exception} and field as a subject of the exception.
     * <p>
     * If the method returns an empty optional then the original exception will
     * be thrown in the place where it has been caught.
     * <p>
     * The produced exception will be thrown instead of the {@code exception}
     * and may contain it as a cause and additional information based on the
     * {@code field}.
     *
     * @param field
     *            the subject of the exception
     * @param exception
     *            an exception thrown within binding logic
     * @return an optional {@link BindingException}, or an empty optional if no
     *         additional information should be provided for the thrown
     *         exception
     */
    Optional<BindingException> handleException(HasValue<?, ?> field,
            Exception exception);
}
