package com.vaadin.flow.spring.springnative;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.io.ClassPathResource;

import com.vaadin.flow.di.LookupInitializer;
import com.vaadin.flow.router.internal.DefaultErrorHandler;

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

        // Random classes that need reflection
        for (String cls : getClasses()) {
            hints.reflection().registerType(TypeReference.of(cls),
                    MemberCategory.values());

        }
        for (String componentClass : getCommonComponentClasses()) {
            hints.reflection().registerType(TypeReference.of(componentClass),
                    MemberCategory.values());
        }
        registerResourceIfPresent(hints,
                "com/vaadin/flow/component/login/i18n.json");

        // Flow server resources like BootstrapHandler.js and
        // RouteNotFoundError_prod.html
        hints.resources().registerPattern("com/vaadin/flow/server/*");
        hints.resources().registerPattern("com/vaadin/flow/router/*");
    }

    private void registerResourceIfPresent(RuntimeHints hints, String path) {
        ClassPathResource res = new ClassPathResource(path);
        if (res.exists()) {
            hints.resources().registerResource(res);
        }

    }

    // These should really go into the separate components but are here for now
    // to ease testing
    private String[] getCommonComponentClasses() {
        return new String[] { "com.vaadin.flow.component.login.LoginI18n",
                "com.vaadin.flow.component.login.LoginI18n$Form",
                "com.vaadin.flow.component.login.LoginI18n$ErrorMessage",
                "com.vaadin.flow.component.messages.MessageListItem" };
    }

    private String[] getClasses() {
        return new String[] {
                "org.apache.catalina.core.ApplicationContextFacade",
                "org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler$SupplierCsrfToken",
                "com.fasterxml.jackson.databind.ser.std.ToStringSerializer",
                DefaultErrorHandler.class.getName() };
    }

}
