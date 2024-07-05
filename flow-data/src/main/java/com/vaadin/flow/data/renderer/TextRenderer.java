/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
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
