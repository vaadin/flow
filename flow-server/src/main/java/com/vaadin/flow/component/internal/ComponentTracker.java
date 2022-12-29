package com.vaadin.flow.component.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.router.internal.AbstractNavigationStateRenderer;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

/**
 * Tracks the location in source code where components were instantiated.
 *
 **/
public class ComponentTracker {

    private static Map<Component, StackTraceElement> createLocation = Collections
            .synchronizedMap(new WeakHashMap<>());
    private static Map<Component, StackTraceElement> attachLocation = Collections
            .synchronizedMap(new WeakHashMap<>());

    private static Set<String> classesToSkip = new HashSet<>();
    static {
        classesToSkip.add(Component.class.getName());
        classesToSkip.add(ComponentTracker.class.getName());
        classesToSkip.add(Thread.class.getName());
        classesToSkip.add("com.vaadin.flow.spring.SpringInstantiator");
        classesToSkip.add(Instantiator.class.getName());
        classesToSkip.add(ReflectTools.class.getName());
    }

    /**
     * Finds the location where the given component instance was created.
     *
     * @param component
     *            the component to find
     * @return an element from the stack trace describing the relevant location
     *         where the component was created
     */
    public static StackTraceElement findCreate(Component component) {
        return createLocation.get(component);
    }

    /**
     * Tracks the location where the component was created. This should be
     * called from the Component constructor so that the creation location can
     * be found from the current stacktrace.
     *
     * @param component
     *            the component to track
     */
    public static void trackCreate(Component component) {
        if (isProductionMode()) {
            return;
        }
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        StackTraceElement location = findRelevantElement(component.getClass(),
                stack);
        if (isNavigatorCreate(location)) {
            location = findRelevantElement(null, stack);
        }
        createLocation.put(component, location);
    }

    /**
     * Finds the location where the given component instance was attached to a
     * parent.
     *
     * @param component
     *            the component to find
     * @return an element from the stack trace describing the relevant location
     *         where the component was attached
     */
    public static StackTraceElement findAttach(Component component) {
        return attachLocation.get(component);
    }

    /**
     * Tracks the location where the component was attached. This should be
     * called from the Component attach logic so that the creation location can
     * be found from the current stacktrace.
     *
     * @param component
     *            the component to track
     */
    public static void trackAttach(Component component) {
        if (isProductionMode()) {
            return;
        }
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        StackTraceElement location = findRelevantElement(component.getClass(),
                stack);
        if (isNavigatorCreate(location)) {
            // For routes, we can just show the init location as we have nothing
            // better
            location = createLocation.get(component);
        }
        attachLocation.put(component, location);
    }

    private static boolean isNavigatorCreate(StackTraceElement location) {
        return location.getClassName()
                .equals(AbstractNavigationStateRenderer.class.getName());
    }

    private static StackTraceElement findRelevantElement(
            Class<? extends Component> excludeClass,
            StackTraceElement[] stack) {
        return Stream.of(stack)
                .filter(e -> !classesToSkip.contains(e.getClassName()))
                .filter(e -> excludeClass == null
                        || !e.getClassName().equals(excludeClass.getName()))
                .filter(e -> !e.getClassName()
                        .startsWith("com.vaadin.flow.component."))
                .filter(e -> !e.getClassName()
                        .startsWith("com.vaadin.flow.internal."))
                .filter(e -> !e.getClassName()
                        .startsWith("com.vaadin.flow.dom."))
                .filter(e -> !e.getClassName().startsWith("java."))
                .filter(e -> !e.getClassName().startsWith("jdk."))
                .filter(e -> !e.getClassName()
                        .startsWith("org.springframework.beans."))
                .findFirst().orElse(null);
    }

    /**
     * Checks if the application is running in production mode.
     *
     * When unsure, reports that production mode is true so tracking does not
     * take place in production.
     *
     * @return true if in production mode or the mode is unclear, false if in
     *         development mode
     **/
    private static boolean isProductionMode() {
        VaadinService service = VaadinService.getCurrent();
        if (service == null) {
            // Rather fall back to not tracking if we are unsure, so we do not
            // use memory in production
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
