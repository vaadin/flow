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
package com.vaadin.ui;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.hummingbird.dom.Element;

public class ComponentUtilTest {

    @Test
    public void attachedToComponent() {
        Component c = Mockito.mock(Component.class);
        Element e = new Element("e");
        e.setComponent(c);
        Assert.assertTrue(ComponentUtil.isAttachedTo(c, e));
    }

    @Test
    public void notAttachedToComponent() {
        Component c = Mockito.mock(Component.class);
        Element e = new Element("e");
        Assert.assertFalse(ComponentUtil.isAttachedTo(c, e));
    }
}
