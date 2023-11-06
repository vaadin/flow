package com.vaadin.gradle;

import kotlin.jvm.functions.Function1;
import org.gradle.api.Transformer;
import org.jetbrains.annotations.NotNull;

public class FunctionToTransformerAdapter<IN, OUT> implements Transformer<OUT, IN> {
    @NotNull
    private final Function1<IN, OUT> f;

    public FunctionToTransformerAdapter(@NotNull Function1<IN, OUT> f) {
        this.f = f;
    }

    @Override
    public OUT transform(@NotNull IN in) {
        // workaround for https://github.com/gradle/gradle/issues/12388 - because of this bug
        // we can't return null from Transformer in Kotlin. Therefore, we'll call
        // a Kotlin function instead, which can return null safely.
        return f.invoke(in);
    }
}
