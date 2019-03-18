/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.data.renderer;

import java.io.Serializable;
import java.util.Optional;

import com.vaadin.flow.data.provider.DataGenerator;
import com.vaadin.flow.dom.Element;

/**
 * Defines the context of a given {@link Renderer} when building the output
 * elements. Components that support Renderers can use the context to customize
 * the rendering according to their needs.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <SOURCE>
 *            the type of the object model
 *
 * @see Renderer#render(Element, com.vaadin.flow.data.provider.DataKeyMapper)
 */
public interface Rendering<SOURCE> extends Serializable {

    /**
     * Gets a {@link DataGenerator} associated with the renderer. The
     * DataGenerator is used in components that support in asynchronous loading
     * of items.
     *
     * @return the associated DataGenerator, if any
     */
    Optional<DataGenerator<SOURCE>> getDataGenerator();

    /**
     * Gets the {@code <template>} element associated with the rendering. This
     * can be used to set specific attributes to the template, or change its
     * contents before it is stamped on the client-side.
     *
     * @return the associated template element, or {@code null} if no template
     *         element is associated with the rendering
     */
    Element getTemplateElement();

}
