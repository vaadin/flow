package com.vaadin.flow.server.frontend.scanner;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mockito.Mockito;

import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.PwaConfiguration;

public class FullScannerPwaTest extends AbstractScannerPwaTest {
    private ClassFinder finder = Mockito.mock(ClassFinder.class);

    protected PwaConfiguration getPwaConfiguration(Class<?>... classes)
            throws Exception {
        // use this fake/mock class for the loaded class to check that annotated
        // classes are requested for the loaded class and not for the
        // annotationType
        Class clazz = Object.class;

        Mockito.doReturn(clazz).when(finder).loadClass(PWA.class.getName());

        Mockito.doReturn(getPwaAnnotatedClasses(classes)).when(finder)
                .getAnnotatedClasses(clazz);

        FullDependenciesScanner fullDependenciesScanner = new FullDependenciesScanner(
                finder, (type, annotation) -> findPwaAnnotations(type), null,
                true);
        return fullDependenciesScanner.getPwaConfiguration();
    }

    private Set<Class<?>> getPwaAnnotatedClasses(Class<?>[] classes) {
        Set<Class<?>> result = new HashSet<>();
        for (Class<?> clazz : classes) {
            if (clazz.getAnnotationsByType(PWA.class).length > 0) {
                result.add(clazz);
            }
        }

        return result;
    }

    private List<? extends Annotation> findPwaAnnotations(Class<?> type) {
        return Arrays.asList(type.getAnnotationsByType(PWA.class));
    }
}
