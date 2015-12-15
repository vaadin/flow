package com.vaadin.hummingbird.kernel;

import java.util.function.Function;
import java.util.function.Supplier;

public interface ModelContext {
    public Function<String, Supplier<Object>> getBindingFactory(StateNode node);
}