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
package com.vaadin.flow.data.validator;

import org.junit.jupiter.api.Test;

class RegexpValidatorTest extends ValidatorTestBase {

    @Test
    void testNullStringFails() {
        assertPasses(null, new RegexpValidator("Should be 'abc'", "abc"));
    }

    @Test
    void testEmptyPatternMatchesEmptyString() {
        assertPasses("", new RegexpValidator("Should be empty", "", true));
    }

    @Test
    void testEmptyPatternDoesNotMatchNonEmptyString() {
        assertFails("x", new RegexpValidator("Should be empty", "", true));
    }

    @Test
    void testPatternMatchesString() {
        RegexpValidator v = new RegexpValidator(
                "Should be foo and bar repeating", "(foo|bar)+", true);

        assertPasses("foo", v);
        assertPasses("barfoo", v);
        assertPasses("foobarbarbarfoobarfoofoobarbarfoofoofoobar", v);
    }

    @Test
    void testPatternDoesNotMatchString() {
        RegexpValidator v = new RegexpValidator(
                "Should be foo and bar repeating", "(foo|bar)+", true);

        assertFails("", v);
        assertFails("barf", v);
        assertFails(" bar", v);
        assertFails("foobarbarbarfoobar.foofoobarbarfoofoofoobar", v);
    }

    @Test
    void testEmptyPatternFoundInAnyString() {
        RegexpValidator v = new RegexpValidator("Should always pass", "",
                false);

        assertPasses("", v);
        assertPasses("      ", v);
        assertPasses("qwertyuiopasdfghjklzxcvbnm", v);
    }

    @Test
    void testPatternFoundInString() {
        RegexpValidator v = new RegexpValidator("Should contain a number",
                "\\d+", false);

        assertPasses("0", v);
        assertPasses("     123     ", v);
        assertPasses("qwerty9iop", v);
    }

    @Test
    void testPatternNotFoundInString() {
        RegexpValidator v = new RegexpValidator("Should contain a number",
                "\\d+", false);

        assertFails("", v);
        assertFails("qwertyiop", v);
    }
}
