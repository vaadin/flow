package com.vaadin.hummingbird.kernel;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;

import com.vaadin.ui.Template.Model;

public class DefaultMethodComputedProperty extends ComputedProperty {
    private final Class<?> ownerType;
    private final Method method;

    public DefaultMethodComputedProperty(String name, Class<?> ownerType,
            Method method) {
        super(name, null);
        this.ownerType = ownerType;
        this.method = method;
    }

    @Override
    public Object compute(StateNode context) {
        Object model = Model.wrap(context, ownerType);
        try {
            // Invoke the default method implementation instead
            // of triggering the proxy handler
            // http://zeroturnaround.com/rebellabs/recognize-and-conquer-java-proxies-default-methods-and-method-handles/
            Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
                    .getDeclaredConstructor(Class.class, int.class);
            constructor.setAccessible(true);
            return constructor
                    .newInstance(ownerType, MethodHandles.Lookup.PRIVATE)
                    .unreflectSpecial(method, ownerType).bindTo(model)
                    .invokeWithArguments();

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof DefaultMethodComputedProperty) {
            DefaultMethodComputedProperty that = (DefaultMethodComputedProperty) obj;
            return getName().equals(that.getName())
                    && ownerType == that.ownerType
                    && method.equals(that.method);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), ownerType, method);
    }
}