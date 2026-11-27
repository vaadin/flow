/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.tests.data.bean;

/**
 * Bean types that are not public, declared outside the binder package so that
 * reflective access from the binder classes is checked against the class
 * visibility.
 */
public final class NonPublicBeans {

    public static final Class<?> BEAN_TYPE = Bean.class;

    public static final Class<?> RECORD_TYPE = Record.class;

    private NonPublicBeans() {
    }

    public static Object newBeanWithChild() {
        Bean bean = new Bean();
        bean.setChild(new Bean());
        return bean;
    }

    public static Object newRecord(String name) {
        return new Record(name);
    }

    static class Bean {
        private String name;
        private Bean child;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Bean getChild() {
            return child;
        }

        public void setChild(Bean child) {
            this.child = child;
        }
    }

    record Record(String name) {
    }
}
