/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.spring.springnative;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.spring.VaadinConfigurationProperties;

/**
 * AOT processor that registers reflection hints for types used in
 * {@link ClientCallable} methods.
 * <p>
 * This processor scans component classes for methods annotated with
 * {@code @ClientCallable} and registers reflection hints for all types used in
 * their signatures. This ensures that parameter and return types can be
 * properly serialized and deserialized when the application runs as a native
 * image.
 * <p>
 * The processor handles complex generic types including parameterized types,
 * wildcards, and type variables, recursively extracting all concrete types that
 * require reflection access.
 *
 * @see ClientCallable
 * @see BeanFactoryInitializationAotProcessor
 */
public class ClientCallableAotProcessor
        implements BeanFactoryInitializationAotProcessor {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ClientCallableAotProcessor.class);

    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(
            ConfigurableListableBeanFactory beanFactory) {

        InspectionResult inspectionResult = new InspectionResult();
        Collection<String> packagesToScan = getPackagesToScan(beanFactory);
        LOGGER.info("Scanning packages {} for @ClientCallable methods",
                packagesToScan);
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
                false);
        configureScanner(scanner);
        for (String packageName : packagesToScan) {
            Set<BeanDefinition> candidates = scanner
                    .findCandidateComponents(packageName);
            LOGGER.debug("Found {} candidate components for package {}",
                    candidates.size(), packageName);
            for (BeanDefinition bd : candidates) {
                if (bd.getBeanClassName() != null) {
                    LOGGER.debug(
                            "Inspecting component class {} for @ClientCallable methods",
                            bd.getBeanClassName());
                    try {
                        Class<?> clazz = ClassUtils.forName(
                                bd.getBeanClassName(),
                                beanFactory.getBeanClassLoader());
                        processClass(clazz, inspectionResult);
                    } catch (ClassNotFoundException e) {
                        LOGGER.warn("Could not load class {}",
                                bd.getBeanClassName(), e);
                    }
                }
            }
        }

        if (inspectionResult.callableMethods.isEmpty()) {
            LOGGER.debug("No @ClientCallable to register for reflection found");
            return null;
        }

        LOGGER.trace("Found @ClientCallable to register for reflection: {}",
                inspectionResult.callableMethods);
        if (!inspectionResult.usedTypes.isEmpty()) {
            LOGGER.debug(
                    "Found @ClientCallable types to register for reflection: {}",
                    inspectionResult.usedTypes);
        }
        return (generationContext, beanFactoryInitializationCode) -> {
            ReflectionHints reflectionHints = generationContext
                    .getRuntimeHints().reflection();

            // Recursively register all types that require reflection hints
            if (!inspectionResult.usedTypes.isEmpty()) {
                BindingReflectionHintsRegistrar registrar = new BindingReflectionHintsRegistrar();
                registrar.registerReflectionHints(reflectionHints,
                        inspectionResult.usedTypes.toArray(new Class[0]));
            }

            inspectionResult.callableMethods.forEach(callable -> reflectionHints
                    .registerType(callable.declaringClass(),
                            b -> b.withMethod(callable.methodName(),
                                    callable.parameterTypes(),
                                    ExecutableMode.INVOKE)));
        };
    }

    private record ClientCallableMethod(Class<?> declaringClass,
            String methodName, List<TypeReference> parameterTypes) {
        ClientCallableMethod(Method method) {
            this(method.getDeclaringClass(), method.getName(),
                    TypeReference.listOf(method.getParameterTypes()));
        }
    }

    private static class InspectionResult {
        Set<Class<?>> usedTypes = new HashSet<>();
        Set<ClientCallableMethod> callableMethods = new HashSet<>();
    }

    // Visible for testing
    void configureScanner(ClassPathScanningCandidateComponentProvider scanner) {
        scanner.addIncludeFilter(new AssignableTypeFilter(Component.class));
    }

    /**
     * Gets the list of packages to scan for Vaadin components.
     * <p>
     * This method returns a list of packages that includes:
     * <ul>
     * <li>The com.vaadin package</li>
     * <li>Auto-configuration packages from Spring Boot</li>
     * <li>Allowed packages from vaadin.allowed-packages configuration
     * property</li>
     * </ul>
     *
     * @param beanFactory
     *            the bean factory
     * @return set of packages to scan
     */
    private static Collection<String> getPackagesToScan(
            ConfigurableListableBeanFactory beanFactory) {
        List<String> packages = new ArrayList<>();
        packages.add("com.vaadin");
        packages.addAll(AutoConfigurationPackages.get(beanFactory));

        // Add allowed packages from the configuration if set
        ConfigurableEnvironment environment = beanFactory
                .getBean(ConfigurableEnvironment.class);
        List<String> allowedPackages = VaadinConfigurationProperties
                .getAllowedPackages(environment);
        if (allowedPackages != null && !allowedPackages.isEmpty()) {
            packages.addAll(allowedPackages);
        }

        // Remove duplicates and redundant packages (e.g. ignore com.vaadin.xyz
        // if com.vaadin is already registered)
        packages.sort(Comparator.comparingInt(String::length));
        Set<String> result = new LinkedHashSet<>();
        for (String pkg : packages) {
            if (result.isEmpty() || result.stream().noneMatch(
                    registeredPkg -> pkg.startsWith(registeredPkg + "."))) {
                result.add(pkg);
            }
        }
        return result;
    }

    /**
     * Processes a single class, extracting types from all
     * {@code @ClientCallable} methods.
     *
     * @param clazz
     *            the class to process
     * @param result
     *            the object to accumulate discovered callables and types
     */
    private void processClass(Class<?> clazz, InspectionResult result) {
        // Flow looks for ClientCallable methods only in classes that extend
        // Component
        if (!Component.class.isAssignableFrom(clazz)) {
            return;
        }
        while (Component.class != clazz) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(ClientCallable.class)) {
                    processMethod(method, result);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * Processes a single {@code @ClientCallable} method, extracting types from
     * its return type and parameters.
     *
     * @param method
     *            the method to process
     * @param result
     *            the object to accumulate discovered callables and types
     */
    private void processMethod(Method method, InspectionResult result) {
        LOGGER.debug("Processing @ClientCallable method {}", method);
        result.callableMethods.add(new ClientCallableMethod(method));
        // Process return type
        Type returnType = method.getGenericReturnType();
        processType(returnType, result.usedTypes);

        // Process parameter types
        Type[] paramTypes = method.getGenericParameterTypes();
        for (Type paramType : paramTypes) {
            processType(paramType, result.usedTypes);
        }
    }

    /**
     * Processes a type, resolving all concrete classes that require reflection
     * hints and filtering out types that don't need registration.
     *
     * @param type
     *            the type to process
     * @param types
     *            the set to accumulate types that need reflection hints
     */
    private void processType(Type type, Set<Class<?>> types) {
        Set<Class<?>> typesToRegister = new HashSet<>();
        resolveTypes(type, typesToRegister);

        for (Class<?> typeToRegister : typesToRegister) {
            if (shouldRegisterType(typeToRegister)) {
                types.add(typeToRegister);
            } else {
                LOGGER.trace(
                        "Ignoring @ClientCallable return/parameter type {}",
                        typeToRegister);
            }
        }
    }

    /**
     * Recursively resolves all concrete classes from a generic type.
     * <p>
     * Handles:
     * <ul>
     * <li>Plain classes</li>
     * <li>Parameterized types (e.g., {@code List<String>})</li>
     * <li>Wildcard types (e.g., {@code ? extends Number})</li>
     * <li>Type variables (e.g., {@code T extends Comparable})</li>
     * </ul>
     *
     * @param type
     *            the type to resolve
     * @param result
     *            the set to accumulate resolved classes
     */
    private void resolveTypes(Type type, Set<Class<?>> result) {
        if (type instanceof Class<?> clazz) {
            if (clazz.isArray()) {
                clazz = clazz.getComponentType();
            }
            result.add(clazz);
        } else if (type instanceof ParameterizedType paramType) {

            // Add raw type
            Type rawType = paramType.getRawType();
            if (rawType instanceof Class) {
                result.add((Class<?>) rawType);
            }

            // Process type arguments
            for (Type typeArg : paramType.getActualTypeArguments()) {
                resolveTypes(typeArg, result);
            }

        } else if (type instanceof WildcardType wildcardType) {

            // Process upper bounds (extends)
            for (Type upperBound : wildcardType.getUpperBounds()) {
                resolveTypes(upperBound, result);
            }

            // Process lower bounds (super)
            for (Type lowerBound : wildcardType.getLowerBounds()) {
                resolveTypes(lowerBound, result);
            }

        } else if (type instanceof TypeVariable<?> typeVar) {

            // Process bounds
            for (Type bound : typeVar.getBounds()) {
                resolveTypes(bound, result);
            }
        } else if (type instanceof GenericArrayType arrayType) {
            resolveTypes(arrayType.getGenericComponentType(), result);
        }
    }

    /**
     * Determines whether a type should be registered for reflection hints.
     * <p>
     * Filters out types that don't need registration:
     * <ul>
     * <li>Primitive types</li>
     * <li>Void</li>
     * <li>Types from standard packages ({@code java.*}, {@code javax.*},
     * {@code jakarta.*})</li>
     * <li>Array types</li>
     * </ul>
     *
     * @param type
     *            the type to check
     * @return {@code true} if the type should be registered, {@code false}
     *         otherwise
     */
    private boolean shouldRegisterType(Class<?> type) {
        // Ignore primitive types
        if (type.isPrimitive()) {
            return false;
        }

        // Ignore void
        if (type == Void.class || type == void.class) {
            return false;
        }

        // Ignore common types
        String packageName = type.getPackageName();
        if (packageName.startsWith("java.") || packageName.startsWith("javax.")
                || packageName.startsWith("jakarta.")
                || packageName.startsWith("tools.jackson.")) {
            return false;
        }

        // Ignore array types (register component type instead)
        if (type.isArray()) {
            return false;
        }

        return true;
    }
}
