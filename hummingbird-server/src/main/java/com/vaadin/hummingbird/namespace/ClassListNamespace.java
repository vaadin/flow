/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.namespace;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

import com.vaadin.hummingbird.StateNode;

/**
 * Namespace for CSS class names for an element.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ClassListNamespace extends SerializableListNamespace<String> {

    /**
     * Provides access to the namespace contents as a set of strings.
     */
    private static class ClassListSetView extends AbstractSet<String>
            implements Serializable {

        private ClassListNamespace namespace;

        private ClassListSetView(ClassListNamespace namespace) {
            this.namespace = namespace;
        }

        @Override
        public int size() {
            return namespace.size();
        }

        @Override
        public void clear() {
            namespace.clear();
        }

        @Override
        public boolean add(String className) {
            verifyClassName(className);

            if (contains(className)) {
                return false;
            }

            namespace.add(size(), className);
            return true;
        }

        @Override
        public boolean remove(Object o) {
            verifyClassName(o);
            return super.remove(o);
        }

        @Override
        public boolean contains(Object o) {
            verifyClassName(o);

            return namespace.indexOf((String) o) != -1;
        }

        private static void verifyClassName(Object className) {
            if (className == null) {
                throw new IllegalArgumentException("Class name cannot be null");
            }

            if (!(className instanceof String)) {
                throw new IllegalArgumentException(
                        "Class name must be a string");
            }

            if (((String) className).indexOf(' ') != -1) {
                throw new IllegalArgumentException(
                        "Class name cannot contain spaces");
            }
        }

        @Override
        public Iterator<String> iterator() {
            return namespace.iterator();
        }
    }

    /**
     * Creates a new class list namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     */
    public ClassListNamespace(StateNode node) {
        super(node);
    }

    /**
     * Creates a set view into this namespace.
     *
     * @return a set view
     */
    public Set<String> getAsSet() {
        return new ClassListSetView(this);
    }
}
