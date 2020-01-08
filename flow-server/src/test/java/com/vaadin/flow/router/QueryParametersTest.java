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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.router.QueryParameters;

public class QueryParametersTest {

    private Map<String, String> getSimpleInputParameters() {
        Map<String, String> inputParameters = new HashMap<>();
        inputParameters.put("one", "1");
        inputParameters.put("two", "2");
        inputParameters.put("three", "3");
        return inputParameters;
    }

    private Map<String, String[]> getFullInputParameters() {
        Map<String, String[]> inputParameters = new HashMap<>();
        inputParameters.put("one", new String[] { "1", "11" });
        inputParameters.put("two", new String[] { "2", "22" });
        inputParameters.put("three", new String[] { "3" });
        return inputParameters;
    }

    private void checkListsForImmutability(Collection<List<String>> lists) {
        for (List<String> list : lists) {
            try {
                list.add("whatever");
                fail("No list should have been mutable");
            } catch (UnsupportedOperationException expected) {
                // exception expected
            }
        }
    }

    @Test
    public void emptyParameters() {
        QueryParameters emptyParams = QueryParameters.empty();

        assertEquals(Collections.emptyMap(), emptyParams.getParameters());
    }

    @Test
    public void emptyParametersToQueryString() {
        QueryParameters emptyParams = QueryParameters.empty();

        assertEquals("", emptyParams.getQueryString());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void underlyingMapUnmodifiable_empty() {
        QueryParameters.empty().getParameters().put("one",
                Collections.emptyList());
    }

    @Test
    public void simpleParameters() {
        QueryParameters simpleParams = QueryParameters
                .simple(getSimpleInputParameters());

        Map<String, List<String>> expectedFullParams = new HashMap<>();
        expectedFullParams.put("one", Collections.singletonList("1"));
        expectedFullParams.put("two", Collections.singletonList("2"));
        expectedFullParams.put("three", Collections.singletonList("3"));
        assertEquals(expectedFullParams, simpleParams.getParameters());
    }

    @Test
    public void simpleParametersToQueryString() {
        QueryParameters simpleParams = QueryParameters
                .simple(getSimpleInputParameters());

        String queryString = simpleParams.getQueryString();
        assertTrue(queryString.contains("one=1"));
        assertTrue(queryString.contains("two=2"));
        assertTrue(queryString.contains("three=3"));
        assertTrue(queryString.contains("&"));
        assertNumberOfOccurences(queryString, 2, "&");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void underlyingMapUnmodifiable_simple() {
        QueryParameters params = QueryParameters
                .simple(getSimpleInputParameters());
        params.getParameters().put("one", Collections.emptyList());
    }

    @Test
    public void underlyingListsUnmodifiable_simple() {
        checkListsForImmutability(QueryParameters
                .simple(getSimpleInputParameters()).getParameters().values());
    }

    @Test
    public void complexParameters() {
        QueryParameters fullParams = QueryParameters
                .full(getFullInputParameters());

        Map<String, List<String>> expectedFullParams = new HashMap<>();
        expectedFullParams.put("one", Arrays.asList("1", "11"));
        expectedFullParams.put("two", Arrays.asList("2", "22"));
        expectedFullParams.put("three", Collections.singletonList("3"));
        assertEquals(expectedFullParams, fullParams.getParameters());
    }

    @Test
    public void complexParametersToQueryString() {
        QueryParameters fullParams = QueryParameters
                .full(getFullInputParameters());

        String queryString = fullParams.getQueryString();
        assertTrue(queryString.contains("one=1"));
        assertTrue(queryString.contains("one=11"));
        assertTrue(queryString.contains("two=2"));
        assertTrue(queryString.contains("two=22"));
        assertTrue(queryString.contains("three=3"));
        assertNumberOfOccurences(queryString, 4, "&");
    }

    private void assertNumberOfOccurences(String stringToCheck,
            int expectedNumber, String element) {
        assertEquals(1, element.length());
        int actualNumbetOfOccurences = stringToCheck.length()
                - stringToCheck.replace(element, "").length();
        assertEquals(expectedNumber, actualNumbetOfOccurences);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void underlyingMapUnmodifiable_full() {
        QueryParameters.full(getFullInputParameters()).getParameters()
                .put("one", Collections.emptyList());
    }

    @Test
    public void underlyingListsUnmodifiable_full() {
        checkListsForImmutability(QueryParameters.full(getFullInputParameters())
                .getParameters().values());
    }

    @Test
    public void parameterWithoutValue() {
        QueryParameters params = new QueryParameters(
                Collections.singletonMap("foo", Collections.emptyList()));
        Assert.assertEquals("foo", params.getQueryString());

        params = new QueryParameters(
                Collections.singletonMap("foo", Arrays.asList(null, "bar")));
        Assert.assertEquals("foo&foo=bar", params.getQueryString());

        params = new QueryParameters(
                Collections.singletonMap("foo", Arrays.asList("bar", null)));
        Assert.assertEquals("foo=bar&foo", params.getQueryString());
    }

    @Test
    public void parameterWithEmptyValue() {
        QueryParameters fullParams = new QueryParameters(
                Collections.singletonMap("foo", Collections.singletonList("")));
        Assert.assertEquals("foo=", fullParams.getQueryString());
    }
}
