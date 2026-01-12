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
package com.vaadin.signals;

import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class IdTest {
    @Test
    void randomId_isRandom() {
        // Test will fail once every 18 quintillion times or so
        assertNotEquals(Id.random(), Id.random());
    }

    @Test
    void basicJsonSerialization() throws JacksonException {
        ObjectMapper mapper = new ObjectMapper();

        Id id = new Id(4600806552848092835l);
        String jsonString = mapper.writeValueAsString(id);

        assertEquals("\"P9lZLwbMzqM\"", jsonString);

        Id deserialized = mapper.readValue(jsonString, Id.class);

        assertEquals(id, deserialized);
    }

    @Test
    void zeroId_compactJson() throws JacksonException {
        ObjectMapper mapper = new ObjectMapper();

        String jsonString = mapper.writeValueAsString(Id.ZERO);

        assertEquals("\"\"", jsonString);

        Id deserialized = mapper.readValue(jsonString, Id.class);

        assertEquals(Id.ZERO, deserialized);
    }

    @Test
    void comparable() {
        List<Id> ids = List.of(new Id(0), new Id(-42365683), new Id(754));

        List<Id> sorted = ids.stream().sorted().toList();

        assertEquals(List.of(new Id(-42365683), new Id(0), new Id(754)),
                sorted);
    }
}
