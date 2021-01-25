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
package com.vaadin.flow.router;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class RouteParametersTest {

    @Test
    public void getters_provide_correct_values() {
        RouteParameters parameters = getParameters();

        // String getter
        Assert.assertEquals("Wrong value", "foo",
                parameters.get("string").get());
        Assert.assertEquals("Wrong value", "123",
                parameters.get("integer").get());
        Assert.assertEquals("Wrong value", "12345678900",
                parameters.get("long").get());
        Assert.assertEquals("Wrong value", "path/to/foo/bar",
                parameters.get("varargs").get());

        // Integer getter
        Assert.assertEquals("Wrong value", Integer.valueOf(123),
                parameters.getInteger("integer").get());

        // Long getter
        Assert.assertEquals("Wrong value", Long.valueOf(123),
                parameters.getLong("integer").get());
        Assert.assertEquals("Wrong value", Long.valueOf(12345678900L),
                parameters.getLong("long").get());

        // Wildcard getter
        Assert.assertEquals("Wrong value", Arrays.asList("foo"),
                parameters.getWildcard("string"));
        Assert.assertEquals("Wrong value", Arrays.asList("123"),
                parameters.getWildcard("integer"));
        Assert.assertEquals("Wrong value", Arrays.asList("12345678900"),
                parameters.getWildcard("long"));
        Assert.assertEquals("Wrong value",
                Arrays.asList("path", "to", "foo", "bar"),
                parameters.getWildcard("varargs"));
    }

    @Test
    public void getters_provide_empty_values() {
        RouteParameters parameters = getParameters();

        Assert.assertFalse(
                "Getting the String value of a non-existing parameter should return empty Optional",
                parameters.get("foo").isPresent());
        Assert.assertFalse(
                "Getting the Integer value of a non-existing parameter should return empty Optional",
                parameters.getInteger("foo").isPresent());
        Assert.assertFalse(
                "Getting the Long value of a non-existing parameter should return empty Optional",
                parameters.getLong("foo").isPresent());

        Assert.assertTrue(
                "Getting the Wildcard value of a non-existing parameter should return empty List",
                parameters.getWildcard("foo").isEmpty());
    }

    @Test
    public void integer_getter_throws_exception() {
        RouteParameters parameters = getParameters();

        try {
            parameters.getInteger("string");

            Assert.fail("getInteger should not be able to format a string.");
        } catch (NumberFormatException e) {
        }

        try {
            parameters.getInteger("long");

            Assert.fail("getInteger should not be able to format a long.");
        } catch (NumberFormatException e) {
        }

        try {
            parameters.getInteger("varargs");

            Assert.fail("getInteger should not be able to format a varargs.");
        } catch (NumberFormatException e) {
        }
    }

    @Test
    public void long_getter_throws_exception() {
        RouteParameters parameters = getParameters();

        try {
            parameters.getLong("string");

            Assert.fail("getInteger should not be able to format a string.");
        } catch (NumberFormatException e) {
        }

        try {
            parameters.getLong("varargs");

            Assert.fail("getInteger should not be able to format a varargs.");
        } catch (NumberFormatException e) {
        }
    }

    @Test
    public void varargs_initializer_throws_exception() {
        try {
            new RouteParameters(new RouteParam("int", "123"),
                    new RouteParam("int", "123"));

            Assert.fail(
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
