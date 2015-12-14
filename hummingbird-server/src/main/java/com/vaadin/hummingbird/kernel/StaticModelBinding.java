package com.vaadin.hummingbird.kernel;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class StaticModelBinding extends ModelBinding {
    private static final Pattern staticExpressionChars = Pattern
            .compile("[a-zA-Z.]*");

    public static boolean isStaticExpression(String expression) {
        return staticExpressionChars.matcher(expression).matches();
    }

    private final String[] parts;

    public StaticModelBinding(String expression, ModelContext context) {
        super(expression, context);
        parts = expression.split("\\.");
    }

    @Override
    protected Object getValue(
            Function<String, Supplier<Object>> bindingFactory) {
        Supplier<Object> supplier = bindingFactory.apply(parts[0]);
        if (supplier == null) {
            return null;
        }

        Object value = supplier.get();

        // Iterate the rest of the parts
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if ("length".equals(part) && (value instanceof ListNode)) {
                ListNode list = (ListNode) value;
                value = Integer.valueOf(list.size());
            } else if (value instanceof StateNode) {
                StateNode stateNode = (StateNode) value;
                value = stateNode.get(part);
            } else {
                return null;
            }
        }

        return value;
    }

}
