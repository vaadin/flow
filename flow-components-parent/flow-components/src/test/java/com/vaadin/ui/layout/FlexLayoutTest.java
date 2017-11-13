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
package com.vaadin.ui.layout;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.ui.html.Div;
import com.vaadin.ui.html.Label;
import com.vaadin.ui.layout.FlexLayout.Alignment;

public class FlexLayoutTest {

    @Test
    public void replace_nullToComponent_appendAsResult() {
        FlexLayout layout = new FlexLayout();
        layout.add(new Label());
        Div div = new Div();
        layout.replace(null, div);
        Assert.assertEquals(div, layout.getComponentAt(1));
    }

    @Test
    public void replace_componentToNull_removeAsResult() {
        FlexLayout layout = new FlexLayout();
        layout.add(new Label());
        Div div = new Div();
        layout.add(div);
        layout.replace(div, null);
        Assert.assertEquals(1, layout.getComponentCount());
    }

    @Test
    public void replace_keepAlignmentSelf() {
        FlexLayout layout = new FlexLayout();
        Div div = new Div();
        layout.add(div);
        layout.setAlignSelf(Alignment.END, div);

        Label label = new Label();
        layout.replace(div, label);
        Assert.assertEquals(Alignment.END, layout.getAlignSelf(label));
    }

    @Test
    public void replace_keepFlexGrow() {
        FlexLayout layout = new FlexLayout();
        Div div = new Div();
        layout.add(div);
        layout.setFlexGrow(1.1d, div);

        Label label = new Label();
        layout.replace(div, label);
        Assert.assertEquals(1.1d, layout.getFlexGrow(label), Double.MIN_VALUE);
    }
}
