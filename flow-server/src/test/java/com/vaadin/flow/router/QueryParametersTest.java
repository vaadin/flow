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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class QueryParametersTest {

    private final String simpleInputQueryString = "one=1&two=2&three=3&four&five=4%2F5%266%2B7&six=one+%2B+one%20%3D%20two";

    private final String complexInputQueryString = "one=1&one=11&two=2&two=22&three=3&four&five=4%2F5%266%2B7&six=one+%2B+one%20%3D%20two";

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

    @Test
    public void underlyingMapUnmodifiable_empty() {
        assertThrows(UnsupportedOperationException.class, () -> {
            QueryParameters.empty().getParameters().put("one",
                    Collections.emptyList());
        });
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
    public void simpleParametersFromQueryString() {
        QueryParameters simpleParams = QueryParameters
                .fromString(simpleInputQueryString);

        Map<String, List<String>> expectedFullParams = new HashMap<>();
        expectedFullParams.put("one", Collections.singletonList("1"));
        expectedFullParams.put("two", Collections.singletonList("2"));
        expectedFullParams.put("three", Collections.singletonList("3"));
        expectedFullParams.put("four", Collections.singletonList(""));
        expectedFullParams.put("five", Collections.singletonList("4/5&6+7"));
        expectedFullParams.put("six",
                Collections.singletonList("one + one = two"));
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

    @Test
    public void underlyingMapUnmodifiable_simple() {
        assertThrows(UnsupportedOperationException.class, () -> {
            QueryParameters params = QueryParameters
                    .simple(getSimpleInputParameters());
            params.getParameters().put("one", Collections.emptyList());
        });
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
    public void complexParametersFromQueryString() {
        QueryParameters fullParams = QueryParameters
                .fromString(complexInputQueryString);

        Map<String, List<String>> expectedFullParams = new HashMap<>();
        expectedFullParams.put("one", Arrays.asList("1", "11"));
        expectedFullParams.put("two", Arrays.asList("2", "22"));
        expectedFullParams.put("three", Collections.singletonList("3"));
        expectedFullParams.put("four", Collections.singletonList(""));
        expectedFullParams.put("five", Collections.singletonList("4/5&6+7"));
        expectedFullParams.put("six",
                Collections.singletonList("one + one = two"));
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

    @Test
    public void underlyingMapUnmodifiable_full() {
        assertThrows(UnsupportedOperationException.class, () -> {
            QueryParameters.full(getFullInputParameters()).getParameters()
                    .put("one", Collections.emptyList());
        });
    }

    @Test
    public void underlyingListsUnmodifiable_full() {
        checkListsForImmutability(QueryParameters.full(getFullInputParameters())
                .getParameters().values());
    }

    @Test
    public void parameterWithoutValue() {
        QueryParameters params = new QueryParameters(
                Collections.singletonMap("foo", Collections.singletonList("")));
        assertEquals("foo", params.getQueryString());

        params = new QueryParameters(
                Collections.singletonMap("foo", Arrays.asList("", "bar")));
        assertEquals("foo&foo=bar", params.getQueryString());

        params = new QueryParameters(
                Collections.singletonMap("foo", Arrays.asList("bar", "")));
        assertEquals("foo=bar&foo", params.getQueryString());
    }

    @Test
    public void parameterWithEmptyValue() {
        QueryParameters fullParams = new QueryParameters(
                Collections.singletonMap("foo", Collections.singletonList("")));
        assertEquals("foo", fullParams.getQueryString());
    }

    @Test
    public void shortHands() {
        QueryParameters qp1 = QueryParameters.of("foo", "bar");
        Optional<String> singleParameter = qp1.getSingleParameter("foo");
        assertEquals("bar", singleParameter.get());
        assertTrue(qp1.getSingleParameter("bar").isEmpty());

        List<String> parameters = qp1.getParameters("foo");
        assertEquals("bar", parameters.get(0));
        assertEquals(1, parameters.size());
        assertTrue(qp1.getParameters("bar").isEmpty());
    }

    @Test
    public void excluding() {
        Map<String, List<String>> paramMap = new HashMap<>();
        paramMap.put("one", Collections.singletonList("1"));
        paramMap.put("two", Collections.singletonList("2"));
        paramMap.put("three", Collections.singletonList("3"));

        QueryParameters params = new QueryParameters(paramMap);
        QueryParameters newParams = params.excluding("two");

        assertEquals(2, newParams.getParameters().size());
        assertEquals(Collections.singletonList("1"),
                newParams.getParameters("one"));
        assertEquals(Collections.emptyList(), newParams.getParameters("two"));
        assertEquals(Collections.singletonList("3"),
                newParams.getParameters("three"));
    }

    @Test
    public void excludingNone() {
        Map<String, List<String>> paramMap = new HashMap<>();
        paramMap.put("one", Collections.singletonList("1"));
        paramMap.put("two", Collections.singletonList("2"));
        paramMap.put("three", Collections.singletonList("3"));

        QueryParameters params = new QueryParameters(paramMap);
        QueryParameters newParams = params.excluding();

        assertEquals(params, newParams);
    }

    @Test
    public void including() {
        Map<String, List<String>> paramMap = new HashMap<>();
        paramMap.put("one", Collections.singletonList("1"));
        paramMap.put("two", Collections.singletonList("2"));
        paramMap.put("three", Collections.singletonList("3"));

        QueryParameters params = new QueryParameters(paramMap);
        QueryParameters newParams = params.including("three", "two");

        assertEquals(2, newParams.getParameters().size());
        assertEquals(Collections.singletonList("3"),
                newParams.getParameters("three"));
        assertEquals(Collections.singletonList("2"),
                newParams.getParameters("two"));
    }

    @Test
    public void includingNone() {
        Map<String, List<String>> paramMap = new HashMap<>();
        paramMap.put("one", Collections.singletonList("1"));
        paramMap.put("two", Collections.singletonList("2"));
        paramMap.put("three", Collections.singletonList("3"));

        QueryParameters params = new QueryParameters(paramMap);
        QueryParameters newParams = params.including();

        assertEquals(0, newParams.getParameters().size());
    }

    @Test
    public void includingNonExisting() {
        Map<String, List<String>> paramMap = new HashMap<>();
        paramMap.put("one", Collections.singletonList("1"));
        paramMap.put("two", Collections.singletonList("2"));
        paramMap.put("three", Collections.singletonList("3"));

        QueryParameters params = new QueryParameters(paramMap);
        QueryParameters newParams = params.including("four");

        assertEquals(0, newParams.getParameters().size());
    }

    @Test
    public void merging() {
        Map<String, List<String>> paramMap = new HashMap<>();
        paramMap.put("one", Collections.singletonList("1"));
        paramMap.put("two", Collections.singletonList("2"));

        QueryParameters params = new QueryParameters(paramMap);
        QueryParameters newParams = params.merging("three", "3").merging("one",
                "one");

        assertEquals(3, newParams.getParameters().size());
        assertEquals(Collections.singletonList("one"),
                newParams.getParameters("one"));
        assertEquals(Collections.singletonList("2"),
                newParams.getParameters("two"));
        assertEquals(Collections.singletonList("3"),
                newParams.getParameters("three"));
    }

    @Test
    public void mergingMultiValue() {
        Map<String, List<String>> paramMap = new HashMap<>();
        paramMap.put("one", Collections.singletonList("1"));
        paramMap.put("two", Collections.singletonList("2"));

        QueryParameters params = new QueryParameters(paramMap);
        QueryParameters newParams = params.merging("three", "3", "3");

        assertEquals(3, newParams.getParameters().size());
        assertEquals(Collections.singletonList("1"),
                newParams.getParameters("one"));
        assertEquals(Collections.singletonList("2"),
                newParams.getParameters("two"));
        assertEquals(Arrays.asList("3", "3"), newParams.getParameters("three"));
    }

    @Test
    public void mergingAll() {
        Map<String, List<String>> paramMap = new HashMap<>();
        paramMap.put("one", Collections.singletonList("1"));
        paramMap.put("two", Collections.singletonList("2"));

        QueryParameters params = new QueryParameters(paramMap);
        QueryParameters newParams = params
                .mergingAll(Map.of("three", Collections.singletonList("3")))
                .mergingAll(Map.of("one", Collections.singletonList("one")));

        assertEquals(3, newParams.getParameters().size());
        assertEquals(Collections.singletonList("one"),
                newParams.getParameters("one"));
        assertEquals(Collections.singletonList("3"),
                newParams.getParameters("three"));
        assertEquals(Collections.singletonList("2"),
                newParams.getParameters("two"));
    }

    @Test
    public void toStringValidation() {
        String toString = QueryParameters.of("foo", "bar").toString();
        assertEquals("QueryParameters(foo=bar)", toString);
    }

    @Test
    public void equalsAndHashCode() {
        QueryParameters qp1 = QueryParameters.of("foo", "bar");
        QueryParameters qp2 = QueryParameters.fromString("foo=bar");
        QueryParameters qp3 = QueryParameters.fromString("bar=foo");
        assertEquals(qp1, qp2);
        assertNotEquals(qp3, qp2);
        assertEquals(qp1.hashCode(), qp2.hashCode());
    }

    @Test
    public void fromString_emptyString_getsEmptyParameters() {
        QueryParameters params = QueryParameters.fromString("");
        assertEquals(Collections.emptyMap(), params.getParameters());
    }

    @Test
    public void fromString_blankString_getsEmptyParameters() {
        QueryParameters params = QueryParameters.fromString("    ");
        assertEquals(Collections.emptyMap(), params.getParameters());
    }

    @Test
    public void fromString_nullString_getsEmptyParameters() {
        QueryParameters params = QueryParameters.fromString(null);
        assertEquals(Collections.emptyMap(), params.getParameters());
    }
}
