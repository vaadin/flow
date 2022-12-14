package com.vaadin.flow.spring;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import com.vaadin.flow.di.LookupInitializer;

/**
 * Registers runtime hints for Spring 3 native support.
 */
public class VaadinHintsRegistrar implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        ReflectionHints ref = hints.reflection();
        LookupInitializer.getDefaultImplementations()
                .forEach(cls -> ref.registerType(cls, MemberCategory.values()));

        // Bundles, build info etc
        hints.resources().registerPattern("META-INF/VAADIN/*");
        // Flow server resources like BootstrapHandler.js and
        // RouteNotFoundError_prod.html
        hints.resources().registerPattern("com/vaadin/flow/server/*");
    }
}
