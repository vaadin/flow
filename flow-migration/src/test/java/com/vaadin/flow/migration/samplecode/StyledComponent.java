/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.migration.samplecode;

import java.io.Serializable;
import java.util.List;

import com.vaadin.flow.component.dependency.StyleSheet;

@StyleSheet("frontend://styles/foo.css")
@StyleSheet("base://styles/foo1.css")
@StyleSheet("context://styles/foo2.css")
@StyleSheet("styles/bar.css")
@StyleSheet("/styles/bar1.css")
@StyleSheet("styles/src/baz.css")
public class StyledComponent<T extends List<?> & Serializable>
        extends GenericComponent<T, String> {

}
