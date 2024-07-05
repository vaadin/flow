/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.migration.samplecode;

import java.io.Serializable;
import java.util.List;

import com.vaadin.flow.component.dependency.HtmlImport;

@HtmlImport("frontend://foo.html")
@HtmlImport("base://foo1.html")
@HtmlImport("context://foo2.html")
@HtmlImport("bar.html")
@HtmlImport("/bar1.html")
@HtmlImport("src/baz.html")
@HtmlImport("frontend://bower_components/vaadin-button/src/vaadin-button.html")
@HtmlImport("bower_components/vaadin-text-field/src/vaadin-text-field.html")
public class Component1<T extends List<?> & Serializable>
        extends GenericComponent<T, String> {

}
