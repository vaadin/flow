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