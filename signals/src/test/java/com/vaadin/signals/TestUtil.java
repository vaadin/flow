package com.vaadin.signals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.vaadin.signals.impl.SignalTree;
import com.vaadin.signals.impl.Transaction;

public class TestUtil {
    public static SignalCommand writeRootValueCommand(String value) {
        return new SignalCommand.SetCommand(Id.random(), Id.ZERO,
                new TextNode(value));
    }

    public static SignalCommand writeRootValueCommand() {
        return writeRootValueCommand("value");
    }

    public static SignalCommand failingCommand() {
        // Fails because the target node doesn't exist (or very unlikely)
        return new SignalCommand.SetCommand(Id.random(), Id.random(), null);
    }

    public static JsonNode readConfirmedRootValue(SignalTree tree) {
        return tree.confirmed().data(Id.ZERO).get().value();
    }

    public static JsonNode readSubmittedRootValue(SignalTree tree) {
        return tree.submitted().data(Id.ZERO).get().value();
    }

    public static JsonNode readTransactionRootValue(SignalTree tree) {
        return Transaction.getCurrent().read(tree).data(Id.ZERO).get().value();
    }

}
