package com.vaadin.flow.component.internal;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

public class ComponentTracker {

    private static WeakHashMap<Component, StackTraceElement> createLocation = new WeakHashMap<>();

    private static Set<String> classesToSkip = new HashSet<>();
    static {
        classesToSkip.add(Component.class.getName());
        classesToSkip.add(ComponentTracker.class.getName());
        classesToSkip.add(Thread.class.getName());
        classesToSkip.add("com.vaadin.flow.spring.SpringInstantiator");
        classesToSkip.add(Instantiator.class.getName());
        classesToSkip.add(ReflectTools.class.getName());
    }

    public static StackTraceElement findCreate(Component c) {
        return createLocation.get(c);
        // return createLocation.entrySet().stream().filter((entry) ->
        // id.equals(entry.getKey().getId().orElse(null)))
        // .map(entry -> entry.getValue()).findFirst();
    }

    public static void trackCreate(Component component) {
        if (isProductionMode()) {
            return;
        }
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        StackTraceElement location = findRelevantElement(component.getClass(),
                stack);
        createLocation.put(component, location);
    }

    private static StackTraceElement findRelevantElement(
            Class<? extends Component> componentClass,
            StackTraceElement[] stack) {
        return Stream.of(stack)
                .filter(e -> !classesToSkip.contains(e.getClassName()))
                .filter(e -> !e.getClassName().equals(componentClass.getName()))
                .filter(e -> !e.getClassName()
                        .startsWith("com.vaadin.flow.component."))
                .filter(e -> !e.getClassName().startsWith("java.lang.reflect."))
                .filter(e -> !e.getClassName().startsWith("jdk.internal."))
                .filter(e -> !e.getClassName()
                        .startsWith("org.springframework.beans."))
                .findFirst().orElse(null);
    }

    private static boolean isProductionMode() {
        VaadinService service = VaadinService.getCurrent();
        if (service == null) {
            return true;
        }

        VaadinContext context = service.getContext();
        if (context == null) {
            return true;
        }
        ApplicationConfiguration applicationConfiguration = ApplicationConfiguration
                .get(context);
        if (applicationConfiguration == null) {
            return true;
        }

        return applicationConfiguration.isProductionMode();
    }

}
