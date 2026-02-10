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
package com.vaadin.flow.router;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RouteParametersTest {

    @Test
    public void getters_provide_correct_values() {
        RouteParameters parameters = getParameters();

        // String getter
        Assertions.assertEquals("foo", parameters.get("string").get(),
                "Wrong value");
        Assertions.assertEquals("123", parameters.get("integer").get(),
                "Wrong value");
        Assertions.assertEquals("12345678900", parameters.get("long").get(),
                "Wrong value");
        Assertions.assertEquals("path/to/foo/bar",
                parameters.get("varargs").get(), "Wrong value");

        // Integer getter
        Assertions.assertEquals(Integer.valueOf(123),
                parameters.getInteger("integer").get(), "Wrong value");

        // Long getter
        Assertions.assertEquals(Long.valueOf(123),
                parameters.getLong("integer").get(), "Wrong value");
        Assertions.assertEquals(Long.valueOf(12345678900L),
                parameters.getLong("long").get(), "Wrong value");

        // Wildcard getter
        Assertions.assertEquals(Arrays.asList("foo"),
                parameters.getWildcard("string"), "Wrong value");
        Assertions.assertEquals(Arrays.asList("123"),
                parameters.getWildcard("integer"), "Wrong value");
        Assertions.assertEquals(Arrays.asList("12345678900"),
                parameters.getWildcard("long"), "Wrong value");
        Assertions.assertEquals(Arrays.asList("path", "to", "foo", "bar"),
                parameters.getWildcard("varargs"), "Wrong value");
    }

    @Test
    public void getters_provide_empty_values() {
        RouteParameters parameters = getParameters();

        Assertions.assertFalse(parameters.get("foo").isPresent(),
                "Getting the String value of a non-existing parameter should return empty Optional");
        Assertions.assertFalse(parameters.getInteger("foo").isPresent(),
                "Getting the Integer value of a non-existing parameter should return empty Optional");
        Assertions.assertFalse(parameters.getLong("foo").isPresent(),
                "Getting the Long value of a non-existing parameter should return empty Optional");

        Assertions.assertTrue(parameters.getWildcard("foo").isEmpty(),
                "Getting the Wildcard value of a non-existing parameter should return empty List");
    }

    @Test
    public void integer_getter_stringParameter_throws() {
        RouteParameters parameters = getParameters();

        IllegalArgumentException ex = Assertions
                .assertThrows(IllegalArgumentException.class, () -> {

                    parameters.getInteger("string");
                });
        Assertions.assertTrue(ex.getMessage().contains(
                "Couldn't parse 'string' parameter value 'foo' as integer"));
    }

    @Test
    public void integer_getter_longParameter_throws() {
        RouteParameters parameters = getParameters();

        IllegalArgumentException ex = Assertions
                .assertThrows(IllegalArgumentException.class, () -> {

                    parameters.getInteger("long");
                });
        Assertions.assertTrue(ex.getMessage().contains(
                "Couldn't parse 'long' parameter value '12345678900' as integer"));
    }

    @Test
    public void integer_getter_varaargsParameter_throws() {
        RouteParameters parameters = getParameters();

        IllegalArgumentException ex = Assertions
                .assertThrows(IllegalArgumentException.class, () -> {

                    parameters.getInteger("varargs");
                });
        Assertions.assertTrue(ex.getMessage().contains(
                "Couldn't parse 'varargs' parameter value 'path/to/foo/bar' as integer"));
    }

    @Test
    public void long_getter_varaargsParameter_throws() {
        RouteParameters parameters = getParameters();

        IllegalArgumentException ex = Assertions
                .assertThrows(IllegalArgumentException.class, () -> {

                    parameters.getLong("varargs");
                });
        Assertions.assertTrue(ex.getMessage().contains(
                "Couldn't parse 'varargs' parameter value 'path/to/foo/bar' as long"));
    }

    @Test
    public void long_getter_stringParameter_throws() {
        RouteParameters parameters = getParameters();

        IllegalArgumentException ex = Assertions
                .assertThrows(IllegalArgumentException.class, () -> {

                    parameters.getLong("string");
                });
        Assertions.assertTrue(ex.getMessage().contains(
                "Couldn't parse 'string' parameter value 'foo' as long"));
    }

    @Test
    public void varargs_initializer_throws_exception() {
        try {
            new RouteParameters(new RouteParam("int", "123"),
                    new RouteParam("int", "123"));

            Assertions.fail(
                    "RouteParameters initializer should have failed with same parameter defined more than once.");
        } catch (IllegalArgumentException e) {
        }
    }

    private RouteParameters getParameters() {
        return new RouteParameters(new RouteParam("string", "foo"),
                new RouteParam("integer", "123"),
                new RouteParam("long", "12345678900"),
                new RouteParam("varargs", "path/to/foo/bar"));
    }

}
