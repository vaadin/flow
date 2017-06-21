package com.vaadin.flow.demo;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.demo.views.DemoView;

/**
 * Registration initializer that collects all the demo views available
 */
@HandlesTypes(ComponentDemo.class)
public class ComponentDemoRegister implements ServletContainerInitializer {

    private static List<Class<? extends DemoView>> availableViews = new ArrayList<>();

    @Override
    public void onStartup(Set<Class<?>> set, ServletContext servletContext)
            throws ServletException {
        set.forEach(clazz -> {
            if (DemoView.class.isAssignableFrom(clazz)) {
                availableViews.add((Class<? extends DemoView>) clazz);
            }
        });
        Collections.sort(availableViews, Comparator.comparing(Class::getName));
    }

    public static List<Class<? extends DemoView>> getAvailableViews() {
        return new ArrayList<>(availableViews);
    }
}
