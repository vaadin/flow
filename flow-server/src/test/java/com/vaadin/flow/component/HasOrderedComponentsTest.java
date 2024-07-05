/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.component;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.dom.Element;

public class HasOrderedComponentsTest {

    static class TestOrderedComponents implements HasOrderedComponents {

        @Override
        public Element getElement() {
            return null;
        }

        @Override
        public Stream<Component> getChildren() {
            return null;
        }

    }

    private HasOrderedComponents components = Mockito
            .spy(TestOrderedComponents.class);

    @Test
    public void indexOf_componentIsChild_returnsIndexOfChild() {
        Component comp = Mockito.mock(Component.class);
        Mockito.when(components.getChildren())
                .thenReturn(Arrays.asList(Mockito.mock(Component.class), comp,
                        Mockito.mock(Component.class)).stream());

        Assert.assertEquals(1, components.indexOf(comp));
    }

    @Test
    public void indexOf_componentIsNotChild_returnsNegative() {
        Component comp = Mockito.mock(Component.class);
        Mockito.when(components.getChildren())
                .thenReturn(Arrays.asList(Mockito.mock(Component.class),
                        Mockito.mock(Component.class)).stream());

        Assert.assertEquals(-1, components.indexOf(comp));
    }

    @Test(expected = IllegalArgumentException.class)
    public void indexOf_componentIsNull_throws() {
        Mockito.when(components.getChildren()).thenReturn(Stream.empty());

        components.indexOf(null);
    }

    @Test
    public void getComponentCount_returnsChildrenSize() {
        Mockito.when(components.getChildren())
                .thenReturn(Arrays.asList(Mockito.mock(Component.class),
                        Mockito.mock(Component.class)).stream());
        Assert.assertEquals(2, components.getComponentCount());
    }

    @Test
    public void getComponentAt_returnsComponentAtIndex() {
        Component comp = Mockito.mock(Component.class);
        Mockito.when(components.getChildren())
                .thenReturn(Arrays.asList(Mockito.mock(Component.class), comp,
                        Mockito.mock(Component.class)).stream());

        Assert.assertSame(comp, components.getComponentAt(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getComponentAt_negativeIndex_throws() {
        Mockito.when(components.getChildren())
                .thenReturn(Arrays.asList(Mockito.mock(Component.class),
                        Mockito.mock(Component.class)).stream());

        components.getComponentAt(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getComponentAt_indexIsGreaterThanSize_throws() {
        Mockito.when(components.getChildren())
                .thenReturn(Stream.of(Mockito.mock(Component.class)));

        components.getComponentAt(-2);
    }
}
