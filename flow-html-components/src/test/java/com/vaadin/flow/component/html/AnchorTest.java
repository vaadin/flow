/*
 * Copyright 2000-2020 Vaadin Ltd.
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
        Anchor anchor = new Anchor("#", new Text("Home"));
        Assert.assertEquals(anchor.getElement().getAttribute("href"), "#");
        Assert.assertEquals(anchor.getHref(), "#");
        Assert.assertEquals(anchor.getElement().getText(), "Home");
        Assert.assertEquals(anchor.getText(), "Home");
    }

    @Test
    public void createWithTarget() {
        Anchor anchor = new Anchor("#", "Home");
        Assert.assertEquals(anchor.getTargetValue(), AnchorTarget.DEFAULT);
        Assert.assertEquals(anchor.getTarget(), Optional.empty());

        anchor.setTarget(AnchorTarget.BLANK);

        Assert.assertEquals(anchor.getTargetValue(), AnchorTarget.BLANK);
        Assert.assertEquals(anchor.getTarget(),
                Optional.of(AnchorTarget.BLANK.getValue()));

        Assert.assertEquals(anchor.getTargetValue(),
                new Anchor("#", "Home", AnchorTarget.BLANK).getTargetValue());
        Assert.assertEquals(anchor.getTarget(),
                new Anchor("#", "Home", AnchorTarget.BLANK).getTarget());
    }

    @Test
    public void shouldNotRemoveRouterIgnoreAttributeWhenRemoveHref() {
        Anchor anchor = new Anchor();
        anchor.getElement().setAttribute("router-ignore", true);
        anchor.removeHref();

        Assert.assertEquals(
                "Anchor element should have router-ignore " + "attribute", "",
                anchor.getElement().getAttribute("router-ignore"));
    }

    @Test
    public void shouldNotBreakBehaviorIfSetHrefWhenHavingRouterIgnoreAttributeBefore() {
        Anchor anchor = new Anchor();
        anchor.getElement().setAttribute("router-ignore", true);
        anchor.setHref("/logout");

        Assert.assertEquals(
                "Anchor element should have router-ignore " + "attribute", "",
                anchor.getElement().getAttribute("router-ignore"));
    }

    @Test
    public void setTargetValue_useEnum_targetIsSet() {
        Anchor anchor = new Anchor();
        anchor.setTarget(AnchorTarget.PARENT);

        Assert.assertEquals(Optional.of(AnchorTarget.PARENT.getValue()),
                anchor.getTarget());
        Assert.assertEquals(AnchorTarget.PARENT, anchor.getTargetValue());
    }

    @Test
    public void setTargetValue_useObject_targetIsSet() {
        Anchor anchor = new Anchor();
        anchor.setTarget(AnchorTargetValue.forString("foo"));

        Assert.assertEquals(Optional.of("foo"), anchor.getTarget());
        Assert.assertEquals("foo", anchor.getTargetValue().getValue());
    }

    @Test
    public void getTargetValue_useEnumStringValue_targetIsReturned() {
        Anchor anchor = new Anchor();
        anchor.setTarget(AnchorTarget.SELF.getValue());

        Assert.assertEquals(Optional.of(AnchorTarget.SELF.getValue()),
                anchor.getTarget());
        Assert.assertEquals(AnchorTarget.SELF, anchor.getTargetValue());
    }

    @Test
    public void getTargetValue_useSomeStringValue_targetIsReturned() {
        Anchor anchor = new Anchor();
        anchor.setTarget("foo");

        Assert.assertEquals(Optional.of("foo"), anchor.getTarget());
        Assert.assertEquals("foo", anchor.getTargetValue().getValue());
    }

    // Other test methods in super class

    @Override
    protected void addProperties() {
        addStringProperty("href", "", false);
        addOptionalStringProperty("target");
    }

}
