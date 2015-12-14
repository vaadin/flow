package com.vaadin.hummingbird.kernel;

import java.util.function.Function;
import java.util.function.Supplier;

public class ScriptModelBinding extends ModelBinding {

    public ScriptModelBinding(String binding, ModelContext context) {
        super(binding, context);
    }

    @Override
    protected Object getValue(
            Function<String, Supplier<Object>> bindingFactory) {
        return TemplateScriptHelper.evaluateScript(bindingFactory, getBinding(),
                Object.class);
    }

}
