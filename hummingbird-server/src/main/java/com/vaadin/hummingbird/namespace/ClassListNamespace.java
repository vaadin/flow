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
import java.util.AbstractList;
import java.util.List;

import com.vaadin.hummingbird.StateNode;

public class ClassListNamespace extends ListNamespace<String> {

    private static class ClassListView extends AbstractList<String>
            implements Serializable {

        private ClassListNamespace namespace;

        public ClassListView(ClassListNamespace namespace) {
            this.namespace = namespace;
        }

        @Override
        public String get(int index) {
            return namespace.get(index);
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
        public void add(int index, String className) {
            verifyClassName(className);

            if (indexOf(className) != -1) {
                return;
            }

            if (index != size()) {
                throw new IllegalArgumentException("Can only add to the end");
            }

            namespace.add(index, className);
        }

        @Override
        public boolean remove(Object o) {
            verifyClassName(o);
            return super.remove(o);
        }

        @Override
        public String remove(int index) {
            return namespace.remove(index);
        }

        @Override
        public boolean contains(Object o) {
            verifyClassName(o);

            return super.contains(o);
        }

        private void verifyClassName(Object className) {
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
    }

    public ClassListNamespace(StateNode node) {
        super(node, false);
    }

    public List<String> getAsList() {
        return new ClassListView(this);
    }
}
