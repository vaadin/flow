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
package com.vaadin.server;

import javax.servlet.ServletException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.annotations.Tag;

/**
 * Test that correct @Tag custom elements get loaded by the initializer loader.
 */
public class FlowCustomElementsTest {

    @Test
    public void testValidCustomElement() throws ServletException {
        FlowCustomElements flowCustomElements = new FlowCustomElements();
        flowCustomElements.onStartup(
                Stream.of(ValidCustomElement.class).collect(Collectors.toSet()),
                null);

        Assert.assertTrue(FlowCustomElements.customElements
                .containsKey("custom-element"));
    }

    @Test
    public void testInvalidCustomElement() throws ServletException {
        FlowCustomElements flowCustomElements = new FlowCustomElements();
        flowCustomElements.onStartup(Stream.of(InvalidCustomElement.class)
                .collect(Collectors.toSet()), null);

        Assert.assertFalse(
                FlowCustomElements.customElements.containsKey("-invalid"));
    }

    @Test
    public void testMultipleTagsWithValidExtends() throws ServletException {
        FlowCustomElements flowCustomElements = new FlowCustomElements();
        flowCustomElements.onStartup(
                Stream.of(ValidCustomElement.class, ValidExtendingElement.class)
                        .collect(Collectors.toSet()),
                null);

        Assert.assertTrue(FlowCustomElements.customElements
                .containsKey("custom-element"));
        Assert.assertEquals("Stored element was not the super class",
                ValidCustomElement.class,
                FlowCustomElements.customElements.get("custom-element"));
    }

    @Test(expected = ClassCastException.class)
    public void testMultipleTagsWithFaultyExtends()throws ServletException {
        FlowCustomElements flowCustomElements = new FlowCustomElements();
        flowCustomElements.onStartup(
                Stream.of(ValidCustomElement.class, InvalidExtendingElement.class)
                        .collect(Collectors.toSet()),
                null);
    }

    @Tag("custom-element")
    public class ValidCustomElement {
    }

    @Tag("custom-element")
    public class ValidExtendingElement extends ValidCustomElement {
    }

    @Tag("-invalid")
    public class InvalidCustomElement {
    }

    @Tag("custom-element")
    public class InvalidExtendingElement {
    }
}