/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.flow.component;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.router.RouterLink;

public class HasValueTest {

    public static class HasValueComponent extends RouterLink implements HasValue<RouterLink, String> {

        @Override
        public void setValue(String value) {

        }

        @Override
        public String getValue() {
            return null;
        }
    }

    public static class HasValueNotComponent implements HasValue<RouterLink, String> {

        @Override
        public void setValue(String value) {

        }

        @Override
        public String getValue() {
            return null;
        }
    }

    @Test
    public void testGetComponent_hasValueIsComponent_defaultWorks() {
        HasValueComponent hasValueComponent = new HasValueComponent();
        Assert.assertEquals("invalid component type", hasValueComponent, hasValueComponent.getComponent());
    }

    @Test(expected = ClassCastException.class)
    public void testGetComponent_notComponent_throwsAnException() {
        new HasValueNotComponent().getComponent();
    }
}
