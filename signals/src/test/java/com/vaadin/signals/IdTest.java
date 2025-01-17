package com.vaadin.signals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class IdTest {
    @Test
    void randomId_isRandom() {
        // Test will fail once every 18 quintillion times or so
        assertNotEquals(Id.random(), Id.random());
    }

    @Test
    void basicJsonSerialization() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        Id id = new Id(4600806552848092835l);
        String jsonString = mapper.writeValueAsString(id);

        assertEquals("\"P9lZLwbMzqM\"", jsonString);

        Id deserialized = mapper.readValue(jsonString, Id.class);

        assertEquals(id, deserialized);
    }

    @Test
    void zeroId_compactJson() throws JsonProcessingException {
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
