/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html;

import java.util.Map;
import java.util.stream.Stream;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;dl&gt;</code> element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Tag(Tag.DL)
public class DescriptionList extends HtmlContainer
        implements ClickNotifier<DescriptionList> {

    /**
     * Component representing a <code>&lt;dt&gt;</code> element.
     *
     * @author Vaadin Ltd
     * @since 1.0
     */
    @Tag(Tag.DT)
    public static class Term extends HtmlContainer
            implements ClickNotifier<Term> {

        /**
         * Creates a new empty term.
         */
        public Term() {
            super();
        }

        /**
         * Creates a new term with the given child components.
         *
         * @param components
         *            the child components
         */
        public Term(Component... components) {
            super(components);
        }

        /**
         * Creates a new term with the given text.
         *
         * @param text
         *            the text
         */
        public Term(String text) {
            super();
            setText(text);
        }
    }

    /**
     * Component representing a <code>&lt;dd&gt;</code> element.
     *
     * @author Vaadin Ltd
     * @since 1.0
     */
    @Tag(Tag.DD)
    public static class Description extends HtmlContainer
            implements ClickNotifier<Description> {

        /**
         * Creates a new empty description.
         */
        public Description() {
            super();
        }

        /**
         * Creates a new description with the given child components.
         *
         * @param components
         *            the child components
         */
        public Description(Component... components) {
            super(components);
        }

        /**
         * Creates a new description with the given text.
         *
         * @param text
         *            the text
         */
        public Description(String text) {
            super();
            setText(text);
        }
    }

    /**
     * Creates a new empty description list.
     */
    public DescriptionList() {
        super();
    }

    /**
     * Creates a new description list with the given map of terms and
     * descriptions.
     *
     * @param terms
     *            the map of terms and descriptions
     */
    public DescriptionList(Map<Term, Description> terms) {
        super(terms.entrySet().stream()
                .flatMap(entry -> Stream.of(entry.getKey(), entry.getValue()))
                .toArray(Component[]::new));
    }
}
