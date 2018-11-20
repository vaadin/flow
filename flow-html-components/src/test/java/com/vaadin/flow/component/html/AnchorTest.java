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
package com.vaadin.flow.component.html;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Text;

public class AnchorTest extends ComponentTest {

    @Test
    public void removeHref() {
        Anchor anchor = new Anchor();
        anchor.setHref("foo");
        Assert.assertTrue(anchor.getElement().hasAttribute("href"));

        anchor.removeHref();
        Assert.assertFalse(anchor.getElement().hasAttribute("href"));
    }

    @Test
    public void createWithComponent() {
        Text text = new Text("Text");
        Label label = new Label("Label");
        Anchor anchor = new Anchor("#", text, label);
        Assert.assertEquals(anchor.getChildren().collect(Collectors.toList()),
                Arrays.asList(text, label));
    }

    // Other test methods in super class

    @Override
    protected void addProperties() {
        addStringProperty("href", "", false);
        addOptionalStringProperty("target");
    }

}
