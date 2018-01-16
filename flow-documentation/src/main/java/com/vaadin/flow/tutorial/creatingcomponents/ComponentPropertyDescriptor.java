/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.tutorial.creatingcomponents;

import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("creating-components/tutorial-component-property-descriptor.asciidoc")
public class ComponentPropertyDescriptor {

    // @formatter:off
    private static PropertyDescriptor<String, String> VALUE =
            PropertyDescriptors.propertyWithDefault("value", "");
    private static PropertyDescriptor<String, Optional<String>> PLACEHOLDER =
            PropertyDescriptors.optionalAttributeWithDefault("placeholder", "");
    // @formatter:on

    class A {
        @Tag("input")
        public class TextField extends Component {

            public String getValue() {
                return get(VALUE);
            }

            public void setValue(String value) {
                set(VALUE, value);
            }
        }
    }

    class B {
        @Tag("input")
        public class TextField extends Component {

            public Optional<String> getPlaceholder() {
                return get(PLACEHOLDER);
            }

            public void setPlaceholder(String placeholder) {
                set(PLACEHOLDER, placeholder);
            }
        }

    }
}
