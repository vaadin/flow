package com.vaadin.flow.component.html;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;code&gt;</code> element.
 *
 * @author Vaadin Ltd
 * @since 25.0
 */
@Tag(Tag.CODE)
public class Code extends HtmlContainer {

    /**
     * Creates a new empty code.
     */
    public Code() {
        super();
    }

    /**
     * Creates a new code with the given child components.
     *
     * @param components
     *            the child components
     */
    public Code(Component... components) {
        super(components);
    }

    /**
     * Creates a new code with the given text.
     *
     * @param text
     *            the text
     */
    public Code(String text) {
        super();
        setText(text);
    }
}
