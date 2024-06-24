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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.migration.samplecode.NonPublicClassWithNestedClass.NestedClassInsideNonPublicClass;

public class ClassUnitWithNonPublicClass {

    public static final String NON_PUBLIC_CLASS_NAME = NonPublicClassWithinForeignUnitFile.class
            .getName();
    public static final String NESTEDCLASS_INSIDE_NON_PUBLIC_CLASS_NAME = NestedClassInsideNonPublicClass.class
            .getName();

}

@HtmlImport("src/foo/bar.html")
class NonPublicClassWithinForeignUnitFile<T extends List<? extends Number> & Serializable, U extends Object>
        extends GenericComponent<T, U> {

}

abstract class NonPublicClassWithNestedClass<T extends Serializable>
        implements List<List<? extends Serializable>> {

    @HtmlImport("baz.html")
    public static class NestedClassInsideNonPublicClass extends Component {

    }
}