/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.flow.component.html;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

public class ImageTest extends ComponentTest {

    // Actual test methods in super class

    @Override
    protected void addProperties() {
        addStringProperty("src", "");
    }

    @Test
    @Override
    public void testHasAriaLabelIsImplemented() {
        super.testHasAriaLabelIsImplemented();
    }

    @Test
    public void emptyAltKeepsAttribute() {
        Image img = new Image("test.png", "");
        Assert.assertEquals("", img.getAlt().get());
        Assert.assertTrue(img.getElement().hasAttribute("alt"));
        img.setAlt(null);
        Assert.assertEquals(Optional.empty(), img.getAlt());
        Assert.assertFalse(img.getElement().hasAttribute("alt"));
    }
}
