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
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.dom.Element;

/**
 * A renderer that renders each item as a text using provided
 * {@link ItemLabelGenerator}.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <ITEM>
 *            the type of the input object that can be used by the rendered
 *            component
 *
 */
public class TextRenderer<ITEM> extends ComponentRenderer<Component, ITEM> {

    private static class TextRendererComponent extends Component {

        TextRendererComponent(Element element) {
            super(element);
        }
    }

    private final ItemLabelGenerator<ITEM> itemLabelGenerator;

    /**
     * Creates a new renderer instance using the default
     * {@link ItemLabelGenerator}: <code>String::valueOf</code>.
     */
    public TextRenderer() {
        this.itemLabelGenerator = String::valueOf;
    }

    /**
     * Creates a new renderer instance using the provided
     * {@code itemLabelGenerator}.
     *
     * @param itemLabelGenerator
     *            the item label generator
     */
    public TextRenderer(ItemLabelGenerator<ITEM> itemLabelGenerator) {
        this.itemLabelGenerator = itemLabelGenerator;
    }

    @Override
    public Component createComponent(ITEM item) {
        String text = itemLabelGenerator.apply(item);
        if (text == null) {
            throw new IllegalStateException(String.format(
                    "Got 'null' as a label value for the item '%s'. "
                            + "'%s' instance may not return 'null' values",
                    item, ItemLabelGenerator.class.getSimpleName()));
        }
        return new TextRendererComponent(createElement(text));
    }

    /**
     * Creates a new {@link Element} that represent the rendered {@code item}.
     * <p>
     * By default the text is wrapped inside a {@code <span>} element.
     * Subclasses may override this method to return some other {@link Element}.
     *
     * @param item
     *            the item to render
     * @return the element representing rendered item
     */
    protected Element createElement(String item) {
        return new Element("span").setText(item);
    }
}
