/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
