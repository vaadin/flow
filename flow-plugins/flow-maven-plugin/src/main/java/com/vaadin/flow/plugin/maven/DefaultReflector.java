package com.vaadin.flow.plugin.maven;

import com.vaadin.flow.server.scanner.ReflectionsClassFinder;
import org.apache.maven.plugin.Mojo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;


public class DefaultReflector implements Reflector {
    protected final ReflectorIsolatedClassLoader isolatedClassLoader;
    protected Object classFinder;

    public DefaultReflector(final ReflectorIsolatedClassLoader isolatedClassLoader) {
        this.isolatedClassLoader = Objects.requireNonNull(isolatedClassLoader);
    }

    public DefaultReflector(final Object copyFromOtherClassLoader) {
        try {
            isolatedClassLoader = Objects.requireNonNull(
                    ReflectTools.getJavaFieldValue(
                            copyFromOtherClassLoader,
                            "isolatedClassLoader",
                            ReflectorIsolatedClassLoader.class));

            classFinder = ReflectTools.getJavaFieldValue(copyFromOtherClassLoader, "classFinder");
        } catch (final Exception e) {
            throw new IllegalArgumentException(
                    "Object of type " + copyFromOtherClassLoader.getClass().getName() + " is not compatible to "
                            + getClass().getName(),
                    e);
        }
    }

    @Override
    public ReflectorIsolatedClassLoader getIsolatedClassLoader() {
        return isolatedClassLoader;
    }

    protected Object getOrCreateClassFinderForIsolatedClassLoader() throws ReflectiveOperationException {
        if (classFinder == null) {
            initClassFinder();
        }
        return classFinder;
    }

    protected synchronized void initClassFinder() throws ReflectiveOperationException {
        if (classFinder == null) {
            final Class<?> classFinderImplClass = getIsolatedClassLoader().loadClass(
                    ReflectionsClassFinder.class.getName());
            classFinder = classFinderImplClass
                    .getConstructor(ClassLoader.class, URL[].class)
                    .newInstance(
                            getIsolatedClassLoader(),
                            getIsolatedClassLoader().urlsToScan());
        }
    }

    @Override
    public Mojo createIsolatedMojo(
            final FlowModeAbstractMojo sourceMojo,
            final Set<String> ignoredFields)
            throws Exception {

        final Class<?> targetMojoClass = getIsolatedClassLoader().loadClass(sourceMojo.getClass().getName());
        final Object targetMojo = targetMojoClass.getConstructor().newInstance();
        copyFields(sourceMojo, targetMojo, ignoredFields);

        ReflectTools.setJavaFieldValue(
                targetMojo,
                FlowModeAbstractMojo.CLASSFINDER_FIELD_NAME,
                getOrCreateClassFinderForIsolatedClassLoader());

        return (Mojo) targetMojo;
    }

    protected void copyFields(
            final FlowModeAbstractMojo sourceMojo,
            final Object targetMojo,
            final Set<String> ignoredFields)
            throws IllegalAccessException, NoSuchFieldException {
        Class<?> sourceClass = sourceMojo.getClass();
        Class<?> targetClass = targetMojo.getClass();
        while (sourceClass != null && sourceClass != Object.class) {
            for (final Field sourceField : Arrays.stream(sourceClass.getDeclaredFields())
                    .filter(f -> !ignoredFields.contains(f.getName()))
                    .toList()) {
                copyField(sourceMojo, targetMojo, sourceField, targetClass);
            }
            targetClass = targetClass.getSuperclass();
            sourceClass = sourceClass.getSuperclass();
        }
    }

    protected void copyField(
            final FlowModeAbstractMojo sourceMojo,
            final Object targetMojo,
            final Field sourceField,
            final Class<?> targetClass)
            throws IllegalAccessException, NoSuchFieldException {
        if (Modifier.isStatic(sourceField.getModifiers())) {
            return;
        }
        sourceField.setAccessible(true);
        final Object value = sourceField.get(sourceMojo);
        if (value == null) {
            return;
        }
        final Field targetField;
        try {
            targetField = targetClass.getDeclaredField(sourceField.getName());
        } catch (final NoSuchFieldException ex) {
            // Should never happen, since the class definition should be the same
            final String message = "Field " + sourceField.getName() + " defined in "
                    + sourceField.getDeclaringClass().getName()
                    + " is missing in " + targetClass.getName();
            sourceMojo.logError(message, ex);
            throw ex;
        }

        final Class<?> targetFieldType = targetField.getType();
        if (!targetFieldType.isAssignableFrom(sourceField.getType())) {
            final String message = "Field " + targetFieldType.getName() + " in class "
                    + targetClass.getName() + " of type "
                    + targetFieldType.getName()
                    + " is loaded from different class loaders."
                    + " Source class loader: "
                    + sourceField.getType().getClassLoader()
                    + ", Target class loader: "
                    + targetFieldType.getClassLoader()
                    + ". This is likely a bug in the Vaadin Maven plugin."
                    + " Please, report the error on the issue tracker.";
            sourceMojo.logError(message);
            throw new NoSuchFieldException(message);
        }
        targetField.setAccessible(true);
        targetField.set(targetMojo, value);
    }
}
