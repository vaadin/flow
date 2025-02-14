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
package com.vaadin.flow.internal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests for the {@link CaseUtil}.
 */
public class CaseUtilTest {

    @Test
    public void upperCaseUnderscoreToHumanFriendly() {
        assertNull(CaseUtil.upperCaseUnderscoreToHumanFriendly(null));
        assertEquals("", CaseUtil.upperCaseUnderscoreToHumanFriendly(""));
        assertEquals("My Bean Container", CaseUtil
                .upperCaseUnderscoreToHumanFriendly("MY_BEAN_CONTAINER"));
        assertEquals("Awesome Url Factory", CaseUtil
                .upperCaseUnderscoreToHumanFriendly("AWESOME_URL_FACTORY"));
        assertEquals("Something",
                CaseUtil.upperCaseUnderscoreToHumanFriendly("SOMETHING"));
    }

    @Test
    public void capitalize() {
        assertNull(CaseUtil.capitalize(null));
        assertEquals("", CaseUtil.capitalize(""));
        assertEquals("Great", CaseUtil.capitalize("great"));
        assertEquals("WONDERFUL", CaseUtil.capitalize("WONDERFUL"));
    }
}
