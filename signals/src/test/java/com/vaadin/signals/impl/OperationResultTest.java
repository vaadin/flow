package com.vaadin.signals.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.vaadin.signals.Id;

public class OperationResultTest {

    @Test
    void rejectAll() {
        Map<Id, OperationResult> in = Map.of(new Id(1), OperationResult.ok(),
                new Id(2), OperationResult.fail("Original"));

        Map<Id, OperationResult> out = OperationResult.rejectAll(in, "New");

        assertEquals(Map.of(new Id(1), OperationResult.fail("New"), new Id(2),
                OperationResult.fail("Original")), out);
    }
}
