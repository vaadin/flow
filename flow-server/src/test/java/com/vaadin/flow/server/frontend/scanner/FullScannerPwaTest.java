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

    @Override
    protected PwaConfiguration getPwaConfiguration(Class<?>... classes)
            throws Exception {
        Mockito.doReturn(getPwaAnnotatedClasses(classes)).when(finder)
                .getAnnotatedClasses(PWA.class);

        FullDependenciesScanner fullDependenciesScanner = new FullDependenciesScanner(
                finder, (type, annotation) -> findPwaAnnotations(type), false);
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
