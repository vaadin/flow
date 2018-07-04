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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.function.SerializableFunction;

/**
 * A renderer that renders each item as a text following by an icon using
 * provided icon generator and label generator.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <ITEM>
 *            the type of the input object that can be used by the rendered
 *            component
 *
 */
public class IconRenderer<ITEM> extends ComponentRenderer<Component, ITEM> {

    private static class IconComponent extends Component
            implements HasComponents {
        IconComponent() {
            super(ElementFactory.createDiv());
        }

        IconComponent(String text) {
            super(Element.createText(text));
        }
    }

    private final SerializableFunction<ITEM, ? extends Component> iconGenerator;

    private final ItemLabelGenerator<ITEM> itemLabelGenerator;

    /**
     * Creates a new renderer instance using the default
     * {@link ItemLabelGenerator}: <code>String::valueOf</code> and the provided
     * {@code iconGenerator}.
     *
     * @param iconGenerator
     *            the icon component generator
     */
    public IconRenderer(
            SerializableFunction<ITEM, ? extends Component> iconGenerator) {
        this(iconGenerator, String::valueOf);
    }

    /**
     * Creates a new renderer instance using the provided {@code iconGenerator}
     * and {@code itemLabelGenerator}.
     *
     * @param iconGenerator
     *            the icon component generator
     * @param itemLabelGenerator
     *            the item label generator
     */
    public IconRenderer(
            SerializableFunction<ITEM, ? extends Component> iconGenerator,
            ItemLabelGenerator<ITEM> itemLabelGenerator) {
        this.iconGenerator = iconGenerator;
        this.itemLabelGenerator = itemLabelGenerator;
    }

    @Override
    public Component createComponent(ITEM item) {
        Component icon = iconGenerator.apply(item);
        if (icon == null) {
            throw new IllegalStateException(String.format(
                    "Got 'null' as an icon for the item '%s'. "
                            + "Icon generator instance may not return 'null' values",
                    item));
        }
        String text = itemLabelGenerator.apply(item);
        if (text == null) {
            throw new IllegalStateException(String.format(
                    "Got 'null' as a label value for the item '%s'. "
                            + "'%s' instance may not return 'null' values",
                    item, ItemLabelGenerator.class.getSimpleName()));
        }
        IconComponent component = new IconComponent();
        component.add(icon);
        component.add(new IconComponent(text));
        return component;
    }
}
