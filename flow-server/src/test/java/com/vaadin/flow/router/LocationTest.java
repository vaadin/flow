/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class LocationTest {
    @Test
    public void parseLocation() {
        Location location = new Location("foo/bar/baz");

        assertEquals(Arrays.asList("foo", "bar", "baz"),
                location.getSegments());
        assertEquals("foo/bar/baz", location.getPath());
    }

    @Test
    public void parseLocationWithEndingSlash() {
        Location location = new Location("foo/bar/");

        assertEquals(Arrays.asList("foo", "bar", ""), location.getSegments());
    }

    @Test
    public void parseLocationStartingWithSlash() {
        Location location = new Location("/foo/bar/");
        assertEquals(Arrays.asList("foo", "bar", ""), location.getSegments());
    }

    @Test
    public void parseNullLocation() {
        Location location = new Location((String) null);
        assertEquals(Arrays.asList(""), location.getSegments());
    }

    @Test
    public void parseNullLocationWithParameters() {
        Location location = new Location((String) null,
                QueryParameters.fromString("foo"));
        assertEquals(Arrays.asList(""), location.getSegments());
    }

    @Test
    public void parseLocationWithQueryStringOnly() {
        Location location = new Location("?hey=hola&zz=");
        assertEquals("", location.getPath());
        Map<String, List<String>> queryMap = new HashMap<>();
        queryMap.put("hey", Collections.singletonList("hola"));
        queryMap.put("zz", Collections.emptyList());

        assertEquals(Collections.singletonList("hola"),
                location.getQueryParameters().getParameters().get("hey"));
        assertEquals(Collections.singletonList(""),
                location.getQueryParameters().getParameters().get("zz"));
    }

    @Test
    public void parseLocationWithQueryString_noValue() {
        Location location = new Location("path?query");

        assertEquals("path", location.getPath());
        assertEquals(
                Collections.singletonMap("query",
                        Collections.singletonList("")),
                location.getQueryParameters().getParameters());
        assertEquals("path?query", location.getPathWithQueryParameters());
    }

    @Test
    public void parseLocationWithQueryString_emptyValue() {
        Location location = new Location("path?query=");

        assertEquals("path", location.getPath());
        assertEquals(
                Collections.singletonMap("query",
                        Collections.singletonList("")),
                location.getQueryParameters().getParameters());
        assertEquals("path?query", location.getPathWithQueryParameters());
    }

    @Test
    public void locationFromSegments() {
        Location location = new Location(Arrays.asList("one", "two"));
        assertEquals(Arrays.asList("one", "two"), location.getSegments());
        assertEquals("one/two", location.getPath());
    }

    @Test
    public void queryValue_decodedCorrectly() {
        QueryParameters queryParameters = new Location("home?value+part")
                .getQueryParameters();
        Assert.assertEquals("'+' should be decoded in map", "value part",
                queryParameters.getParameters().keySet().iterator().next());
        Assert.assertEquals("'+' should not be decoded in query param string",
                "value%20part", queryParameters.getQueryString());

        queryParameters = new Location("home?someValue1%2BsomeValue2")
                .getQueryParameters();
        Assert.assertEquals("'%2B' should be decoded in map",
                "someValue1+someValue2",
                queryParameters.getParameters().keySet().iterator().next());
        Assert.assertEquals("'%2B' should not be decoded in query param string",
                "someValue1%2BsomeValue2", queryParameters.getQueryString());

        queryParameters = new Location("home?%25HF").getQueryParameters();
        Assert.assertEquals("'%25' should be decoded in map", "%HF",
                queryParameters.getParameters().keySet().iterator().next());
        Assert.assertEquals("'%25' should not be decoded in query param string",
                "%25HF", queryParameters.getQueryString());

        queryParameters = new Location("home?p=%26&q=%20").getQueryParameters();
        Assert.assertEquals("'%26' should be decoded in map", "&",
                queryParameters.getParameters().get("p").get(0));
        Assert.assertEquals("'%20' should be decoded in map", " ",
                queryParameters.getParameters().get("q").get(0));
        Assert.assertEquals(
                "'%26' and '%20' should not be decoded in query param string",
                "p=%26&q=%20", queryParameters.getQueryString());
    }

    @Test
    public void subLocation() {
        Location location = new Location(Arrays.asList("one", "two", "three"));

        assertEquals("one", location.getFirstSegment());

        Optional<Location> subLocation = location.getSubLocation();
        assertEquals(Arrays.asList("two", "three"),
                subLocation.get().getSegments());
        assertEquals("two/three", subLocation.get().getPath());

    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyLocation() {
        new Location(Collections.emptyList());
    }

    @Test
    public void noSubLocation_emptyOptional() {
        Location location = new Location("foo");
        Optional<Location> maybeSubLocation = location.getSubLocation();

        Assert.assertFalse(maybeSubLocation.isPresent());
    }

    @Test
    public void spaceInLocation() {
        Location location = new Location("foo bar");
        assertEquals("foo bar", location.getFirstSegment());
    }

    @Test
    public void umlautInLocation() {
        Location location = new Location("foo/åäö/bar");

        assertEquals("foo", location.getSegments().get(0));
        assertEquals("åäö", location.getSegments().get(1));
        assertEquals("bar", location.getSegments().get(2));
    }

    @Test
    public void toggleTrailingSlash() {
        assertEquals("foo",
                new Location("foo/").toggleTrailingSlash().getPath());
        assertEquals("foo/",
                new Location("foo").toggleTrailingSlash().getPath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void toggleTrailingSlash_emtpyLocation() {
        // Does not make sense to change the location to "/"
        new Location("").toggleTrailingSlash();
    }

    @Test
    public void locationWithParametersPath_emptyParams() {
        String initialPath = "foo/bar/";
        Location location = new Location(initialPath);

        String pathWithParameters = location.getPathWithQueryParameters();

        assertEquals(initialPath, pathWithParameters);
        assertEquals(location.getPath(), pathWithParameters);
    }

    @Test
    public void locationWithParametersPath_withTrailingSlash() {
        String initialPath = "foo/bar/";
        QueryParameters queryParams = getQueryParameters();
        Location location = new Location(initialPath, queryParams);

        String pathWithParameters = location.getPathWithQueryParameters();

        assertEquals(initialPath + '?' + queryParams.getQueryString(),
                pathWithParameters);
    }

    @Test
    public void locationWithParametersPath_withoutTrailingSlash() {
        String initialPath = "foo/bar";
        QueryParameters queryParams = getQueryParameters();
        Location location = new Location(initialPath, queryParams);

        String pathWithParameters = location.getPathWithQueryParameters();

        assertEquals(initialPath + '?' + queryParams.getQueryString(),
                pathWithParameters);
    }

    private static QueryParameters getQueryParameters() {
        Map<String, String[]> inputParameters = new HashMap<>();
        inputParameters.put("one", new String[] { "1", "11" });
        inputParameters.put("two", new String[] { "2", "22" });
        inputParameters.put("three", new String[] { "3" });

        return QueryParameters.full(inputParameters);
    }

    @Test
    public void locationWithParamsInUrl() {
        String initialPath = "foo/bar/";
        QueryParameters queryParams = getQueryParameters();
        Location location = new Location(initialPath, queryParams);

        assertEquals("foo/bar/", location.getPath());
        assertEquals(queryParams.getParameters(),
                location.getQueryParameters().getParameters());
    }

    @Test
    public void locationWithParamsInUrlAndParameters() {
        Location location = new Location("foo/bar/?one&two=222",
                getQueryParameters());
        assertEquals(Arrays.asList("foo", "bar", "?one&two=222"),
                location.getSegments());
        Assert.assertEquals(
                "Query parameters should be taken from the constructor parameter, not from path",
                "one=1&one=11&two=2&two=22&three=3",
                location.getQueryParameters().getQueryString());
    }

    @Test
    public void locationWithParamWithAndWithoutValue() {
        Location location = new Location("foo?param&param=bar");
        Assert.assertEquals("param&param=bar",
                location.getQueryParameters().getQueryString());

        location = new Location("foo?param=bar&param");
        Assert.assertEquals("param=bar&param",
                location.getQueryParameters().getQueryString());
    }

    @Test
    public void locationWithParamAndEmptyValue() {
        Location location = new Location("foo?param=&param=bar");

        Assert.assertEquals("param&param=bar",
                location.getQueryParameters().getQueryString());
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void locationNameShouldBeAbleToHaveDotDot() {
        Location location = new Location("..element");
        assertEquals("..element", location.getFirstSegment());

        location = new Location("el..ement");
        assertEquals("el..ement", location.getFirstSegment());
    }

    @Test
    public void locationShouldBeRelative() {
        expectedEx.expect(InvalidLocationException.class);
        expectedEx.expectMessage("Relative path cannot contain .. segments");

        new Location("../element");
    }

    @Test
    public void locationShouldNotEndWithDotDotSegment() {
        expectedEx.expect(InvalidLocationException.class);
        expectedEx.expectMessage("Relative path cannot contain .. segments");

        new Location("element/..");
    }

    @Test
    public void dotDotLocationShouldNotWork() {
        expectedEx.expect(InvalidLocationException.class);
        expectedEx.expectMessage("Relative path cannot contain .. segments");

        new Location("..");
    }

    @Test
    public void pathShouldBeEmpty() {
        assertEquals("", new Location("").getPathWithQueryParameters());
    }

    @Test
    public void locationWithUrlEncodedCharacters() {
        Location location = new Location(
                "foo?bar=a%20b%20%C3%B1%20%26%20%3F&baz=xyz");
        Assert.assertEquals(Arrays.asList("a b ñ & ?"),
                location.getQueryParameters().getParameters().get("bar"));
        Assert.assertEquals(Arrays.asList("xyz"),
                location.getQueryParameters().getParameters().get("baz"));
        Assert.assertEquals("bar=a%20b%20%C3%B1%20%26%20%3F&baz=xyz",
                location.getQueryParameters().getQueryString());
    }

    @Test
    public void colonInLocationPath_locationIsParsed() {
        Location location = new Location("abc:foo/bar?baz");
        Assert.assertEquals("abc:foo/bar", location.getPath());
        Assert.assertEquals("baz",
                location.getQueryParameters().getQueryString());
    }

    @Test
    public void locationWithFragment_fragmentRetainedForPathWithQueryParameters() {
        String locationString = "foo#fragment";
        Location location = new Location(locationString);
        Assert.assertEquals(locationString,
                location.getPathWithQueryParameters());

        locationString = "foo/#fragment";
        location = new Location(locationString);
        Assert.assertEquals(locationString,
                location.getPathWithQueryParameters());

        locationString = "foo?bar#fragment";
        location = new Location(locationString);
        Assert.assertEquals(locationString,
                location.getPathWithQueryParameters());

        locationString = "foo/?bar=baz#fragment";
        location = new Location(locationString);
        Assert.assertEquals(locationString,
                location.getPathWithQueryParameters());

        locationString = "foo#";
        location = new Location(locationString);
        Assert.assertEquals(locationString,
                location.getPathWithQueryParameters());

        locationString = "foo/?bar=baz#";
        location = new Location(locationString);
        Assert.assertEquals(locationString,
                location.getPathWithQueryParameters());
    }
}
