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
package com.vaadin.flow.component.html;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DivTest extends ComponentTest {

    // Actual test methods in super class

    @Override
    protected void addProperties() {
        // Component defines no new properties
    }

    @Test
    @Override
    protected void testHasOrderedComponents() {
        super.testHasOrderedComponents();
    }

    @Test
    @Override
    protected void testHasAriaLabelIsNotImplemented() {
        // Don't use aria-label or aria-labelledby on a span or div unless
        // its given a role. When aria-label or aria-labelledby are on
        // interactive roles (such as a link or button) or an img role,
        // they override the contents of the div or span.
        // Other roles besides Landmarks (discussed above) are ignored.
        // Source: https://www.w3.org/TR/using-aria/#label-support
        super.testHasAriaLabelIsNotImplemented();
    }

    @Test
    void testNonDefaultConstructor() {
        assertEquals("text", new Div("text").getText());
    }

    @Test
    public void testAddTypedCollectionOfComponents() {
        // Test for the GitHub issue fix - should compile and work with typed collections
        Div container = new Div();
        
        // Create a List<Span> (subtype of Component)
        java.util.List<Span> typedSpans = java.util.List
                .of(new Span("span1"), new Span("span2"), new Span("span3"));
        
        // This should now compile with Collection<? extends Component>
        container.add(typedSpans);
        
        Assert.assertEquals(3, container.getChildren().count());
    }

    @Test
    public void testRemoveTypedCollectionOfComponents() {
        // Test for the GitHub issue fix - should compile and work with typed collections
        Div container = new Div();
        
        Span span1 = new Span("span1");
        Span span2 = new Span("span2");
        Span span3 = new Span("span3");
        
        container.add(span1, span2, span3);
        Assert.assertEquals(3, container.getChildren().count());
        
        // Create a List<Span> (subtype of Component)
        java.util.List<Span> typedSpans = java.util.List.of(span1, span2);
        
        // This should now compile with Collection<? extends Component>
        container.remove(typedSpans);
        
        Assert.assertEquals(1, container.getChildren().count());
    }
}
