package com.vaadin.signals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.vaadin.signals.ListSignal.ListPosition;

public class SignalCommandTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private Set<Class<?>> assertedTypes = new HashSet<>();

    @Test
    void json_serializeDeserializeCommands_commandsSerializable() {

        Id id = Id.random();
        ListPosition pos = new ListPosition(id, id);
        JsonNode value = new TextNode("value");
        String key = "key";

        assertSerializable(new SignalCommand.AdoptAsCommand(id, id, id, key));
        assertSerializable(new SignalCommand.AdoptAtCommand(id, id, id, pos));
        assertSerializable(new SignalCommand.ClearCommand(id, id));
        assertSerializable(new SignalCommand.ClearOwnerCommand(id, id));
        assertSerializable(new SignalCommand.IncrementCommand(id, id, 0));
        assertSerializable(
                new SignalCommand.InsertCommand(id, id, id, value, pos));
        assertSerializable(new SignalCommand.KeyCondition(id, id, key, id));
        assertSerializable(new SignalCommand.LastUpdateCondition(id, id, id));
        assertSerializable(
                new SignalCommand.PositionCondition(id, id, id, pos));
        assertSerializable(new SignalCommand.PutCommand(id, id, key, value));
        assertSerializable(
                new SignalCommand.PutIfAbsentCommand(id, id, id, key, value));
        assertSerializable(new SignalCommand.RemoveByKeyCommand(id, id, key));
        assertSerializable(new SignalCommand.RemoveCommand(id, id, id));
        assertSerializable(new SignalCommand.SetCommand(id, id, value));
        assertSerializable(new SignalCommand.SnapshotCommand(id,
                Map.of(Id.random(),
                        new Node.Data(id, id, id, value, List.of(), Map.of()),
                        Id.random(), new Node.Alias(id))));
        assertSerializable(new SignalCommand.TransactionCommand(id, List.of()));

        Stream.of(SignalCommand.class.getPermittedSubclasses())
                .filter(type -> !type.isInterface())
                .filter(type -> !assertedTypes.contains(type))
                .forEach(type -> fail("Should test serialization for " + type));
    }

    private void assertSerializable(SignalCommand command) {
        try {
            String json = mapper.writeValueAsString(command);
            SignalCommand deserialized = mapper.readValue(json,
                    SignalCommand.class);
            assertEquals(command, deserialized);

            assertedTypes.add(command.getClass());
        } catch (JsonProcessingException e) {
            fail(e);
        }
    }
}
