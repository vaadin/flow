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
package com.vaadin.flow.data.provider;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class that verifies that ReplaceListDataProvider functions the way it's
 * meant to.
 *
 */
class ReplaceListDataProviderTest {

    private static final StrBean TEST_OBJECT = new StrBean("Foo", 10, -1);
    private ReplaceListDataProvider dataProvider = new ReplaceListDataProvider(
            new ArrayList<>(Arrays.asList(TEST_OBJECT)));

    @Test
    void testGetIdOfItem() {
        Object id = dataProvider.fetch(new Query<>()).findFirst()
                .map(dataProvider::getId).get();
        assertEquals(TEST_OBJECT.getId(), id,
                "DataProvider not using correct identifier getter");
    }

    @Test
    void testGetIdOfReplacementItem() {
        assertFalse(dataProvider.isStale(TEST_OBJECT),
                "Test object was stale before making any changes.");

        dataProvider.refreshItem(new StrBean("Replacement TestObject", 10, -2));

        StrBean fromDataProvider = dataProvider.fetch(new Query<>()).findFirst()
                .get();
        Object id = dataProvider.getId(fromDataProvider);

        assertNotEquals(TEST_OBJECT, fromDataProvider,
                "DataProvider did not return the replacement");

        assertEquals(TEST_OBJECT.getId(), id,
                "DataProvider not using correct identifier getter");

        assertTrue(dataProvider.isStale(TEST_OBJECT),
                "Old test object should be stale");
    }
}
