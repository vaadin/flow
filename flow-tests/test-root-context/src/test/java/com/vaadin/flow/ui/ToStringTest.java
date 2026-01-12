/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.ui;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.uitest.servlet.ViewClassLocator;

public class ToStringTest {
    @Test
    public void testViewsElementsStringable() throws Exception {
        Collection<Class<? extends Component>> viewClasses = new ViewClassLocator(
                getClass().getClassLoader()).getAllViewClasses();
        for (Class<? extends Component> viewClass : viewClasses) {
            Component view = viewClass.getDeclaredConstructor().newInstance();
            String string = view.getElement().toString();
            Assert.assertNotNull(string);
            Assert.assertNotEquals("", string);
        }
    }

}
