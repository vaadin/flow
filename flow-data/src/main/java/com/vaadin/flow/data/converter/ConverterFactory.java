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
package com.vaadin.flow.data.converter;

import java.io.Serializable;
import java.util.Optional;

/**
 * Creates {@link Converter} instances capable to handle conversion between a
 * model and a presentation type.
 *
 * @author Vaadin Ltd
 * @since
 */
public interface ConverterFactory extends Serializable {

    /**
     * Attempts to create a {@link Converter} instance, capable to handle
     * conversion between the given presentation and model types.
     *
     * An empty {@link Optional} is returned if a conversion cannot be
     * performed.
     *
     * @param presentationType
     *            presentation type, not {@literal null}.
     * @param modelType
     *            model type, not {@literal null}.
     * @param <P>
     *            The presentation type.
     * @param <M>
     *            The model type.
     * @return a {@link Converter} instance wrapped into an {@link Optional}, or
     *         an empty {@link Optional} if no suitable converter is available.
     */
    <P, M> Optional<Converter<P, M>> newInstance(Class<P> presentationType,
            Class<M> modelType);
}
