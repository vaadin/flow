/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.cdi;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.util.AnnotationLiteral;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.cdi.annotation.VaadinServiceEnabled;
import com.vaadin.cdi.util.AnyLiteral;

/**
 * Utility class for CDI lookup, and instantiation.
 * <p>
 * Dependent beans are instantiated without any warning, but do not get
 * destroyed properly. {@link jakarta.annotation.PreDestroy} won't run.
 *
 * @param <T>
 *            Bean Type
 */
class BeanLookup<T> {
    private final BeanManager beanManager;
    private final Class<T> type;
    private final Annotation[] qualifiers;
    private UnsatisfiedHandler unsatisfiedHandler = () -> {
    };
    private Consumer<AmbiguousResolutionException> ambiguousHandler = e -> {
        throw e;
    };

    final static Annotation SERVICE = new ServiceLiteral();
    private final static Annotation[] ANY = new Annotation[] {
            new AnyLiteral() };

    private static class ServiceLiteral
            extends AnnotationLiteral<VaadinServiceEnabled>
            implements VaadinServiceEnabled {

    }

    @FunctionalInterface
    public interface UnsatisfiedHandler {
        void handle();
    }

    BeanLookup(BeanManager beanManager, Class<T> type,
            Annotation... qualifiers) {
        this.beanManager = beanManager;
        this.type = type;
        if (qualifiers.length > 0) {
            this.qualifiers = qualifiers;
        } else {
            this.qualifiers = ANY;
        }
    }

    BeanLookup<T> setUnsatisfiedHandler(UnsatisfiedHandler unsatisfiedHandler) {
        this.unsatisfiedHandler = unsatisfiedHandler;
        return this;
    }

    BeanLookup<T> setAmbiguousHandler(
            Consumer<AmbiguousResolutionException> ambiguousHandler) {
        this.ambiguousHandler = ambiguousHandler;
        return this;
    }

    T lookupOrElseGet(Supplier<T> fallback) {
        final Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);
        if (beans == null || beans.isEmpty()) {
            unsatisfiedHandler.handle();
            return fallback.get();
        }
        final Bean<?> bean;
        try {
            bean = beanManager.resolve(beans);
        } catch (AmbiguousResolutionException e) {
            ambiguousHandler.accept(e);
            return fallback.get();
        }
        final CreationalContext<?> ctx = beanManager
                .createCreationalContext(bean);
        // noinspection unchecked
        return (T) beanManager.getReference(bean, type, ctx);
    }

    T lookup() {
        return lookupOrElseGet(() -> null);
    }
}
