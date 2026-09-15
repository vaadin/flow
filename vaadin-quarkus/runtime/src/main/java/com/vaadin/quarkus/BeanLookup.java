/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.quarkus;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import com.vaadin.quarkus.annotation.VaadinServiceEnabled;

/**
 * Utility class for Quarkus CDI lookup, and instantiation.
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

    private static final Annotation[] ANY = new Annotation[] {
            new AnyLiteral() };

    @FunctionalInterface
    public interface UnsatisfiedHandler {
        void handle();
    }

    BeanLookup(final BeanManager beanManager, final Class<T> type,
            final Annotation... qualifiers) {
        this.beanManager = beanManager;
        this.type = type;
        if (qualifiers.length > 0) {
            this.qualifiers = qualifiers;
        } else {
            this.qualifiers = ANY;
        }
    }

    BeanLookup<T> setUnsatisfiedHandler(
            final UnsatisfiedHandler unsatisfiedHandler) {
        this.unsatisfiedHandler = unsatisfiedHandler;
        return this;
    }

    BeanLookup<T> setAmbiguousHandler(
            final Consumer<AmbiguousResolutionException> ambiguousHandler) {
        this.ambiguousHandler = ambiguousHandler;
        return this;
    }

    T lookupOrElseGet(final Supplier<T> fallback) {
        final Set<Bean<?>> beans = this.beanManager.getBeans(this.type,
                this.qualifiers);
        if (beans == null || beans.isEmpty()) {
            this.unsatisfiedHandler.handle();
            return fallback.get();
        }
        final Bean<?> bean;
        try {
            bean = this.beanManager.resolve(beans);
        } catch (final AmbiguousResolutionException e) {
            this.ambiguousHandler.accept(e);
            return fallback.get();
        }
        final CreationalContext<?> ctx = this.beanManager
                .createCreationalContext(bean);
        // noinspection unchecked
        return (T) this.beanManager.getReference(bean, this.type, ctx);
    }

    T lookup() {
        return lookupOrElseGet(() -> null);
    }

    Stream<T> lookupAll() {
        final Set<Bean<?>> beans = this.beanManager.getBeans(this.type,
                VaadinServiceEnabled.Literal.INSTANCE);
        if (beans == null || beans.isEmpty()) {
            this.unsatisfiedHandler.handle();
            return Stream.empty();
        }
        return beans.stream().map(bean -> {
            final CreationalContext<?> ctx = this.beanManager
                    .createCreationalContext(bean);
            // noinspection unchecked
            return (T) this.beanManager.getReference(bean, this.type, ctx);
        });
    }
}
