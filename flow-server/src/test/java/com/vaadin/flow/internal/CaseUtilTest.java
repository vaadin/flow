/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
