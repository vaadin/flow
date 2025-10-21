package com.vaadin.flow.spring.springnative;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.io.ClassPathResource;

import com.vaadin.flow.di.LookupInitializer;
import com.vaadin.flow.router.MenuData;
import com.vaadin.flow.router.internal.DefaultErrorHandler;
import com.vaadin.flow.server.menu.AvailableViewInfo;
import com.vaadin.flow.server.menu.RouteParamType;

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
        hints.resources().registerPattern("META-INF/VAADIN/**");
        hints.resources().registerPattern("vaadin-i18n/*");
        hints.resources().registerPattern("vaadin-featureflags.properties");

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
        registerResourceIfPresent(hints,
                "com/vaadin/flow/component/crud/i18n.json");

        // Flow server resources like BootstrapHandler.js and
        // RouteNotFoundError_prod.html
        hints.resources().registerPattern("com/vaadin/flow/server/**");
        hints.resources().registerPattern("com/vaadin/flow/router/**");
    }

    private void registerResourceIfPresent(RuntimeHints hints, String path) {
        ClassPathResource res = new ClassPathResource(path);
        if (res.exists()) {
            hints.resources().registerResource(res);
        }

    }

    // These should really go into the separate components but are here for now
    // to ease testing
    private Set<String> getCommonComponentClasses() {
        Set<String> classes = new HashSet<>(
                List.of("com.vaadin.flow.component.messages.MessageListItem"));

        // A common pattern in Flow components is to handle translations in
        // classes with name ending in I18n and their potential inner classes,
        // that are serialized as JSON and sent to the client.
        // An exception is the Upload component whose translations class has
        // capitalized N (UploadI18N)
        Predicate<String> i18nClasses = className -> className
                .matches(".*I18[nN]($|\\$.*$)");
        // Charts and Map configurations are serialized as JSON to be sent to
        // the client. All configuration classes need to be registered for
        // reflection.
        Predicate<String> componentsFilter = i18nClasses
                .or(className -> className
                        .startsWith("com.vaadin.flow.component.charts.model.")
                        || className.startsWith(
                                "com.vaadin.flow.component.map.configuration."));

        classes.addAll(classesFromPackage("com.vaadin.flow.component",
                componentsFilter));
        return classes;
    }

    private static Set<String> classesFromPackage(String packageName,
            Predicate<String> filter) {
        return new Reflections(new ConfigurationBuilder()
                .forPackage(packageName).setScanners(Scanners.SubTypes))
                .getAll(Scanners.SubTypes).stream()
                .filter(cl -> cl.startsWith(packageName)).filter(filter)
                .collect(Collectors.toSet());
    }

    private String[] getClasses() {
        return new String[] {
                "org.apache.catalina.core.ApplicationContextFacade",
                "org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler$SupplierCsrfToken",
                "org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter",
                "org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper",
                "com.fasterxml.jackson.databind.ser.std.ToStringSerializer",
                DefaultErrorHandler.class.getName(), MenuData.class.getName(),
                AvailableViewInfo.class.getName(),
                AvailableViewInfo.DetailDeserializer.class.getName(),
                AvailableViewInfo.DetailSerializer.class.getName(),
                RouteParamType.class.getName() };
    }

}
