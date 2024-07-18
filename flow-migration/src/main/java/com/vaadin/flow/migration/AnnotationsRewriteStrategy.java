/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.migration;

import com.vaadin.flow.component.dependency.StyleSheet;

/**
 * The strategy to rewrite {@link HtmlImport}/{@link StyleSheet} annotations.
 *
 * @since 2.0
 */
public enum AnnotationsRewriteStrategy {
    ALWAYS, SKIP, SKIP_ON_ERROR;
}
