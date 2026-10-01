/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.quarkus.annotation.VaadinServiceEnabled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every optional Vaadin service the extension picks up from the container goes
 * through this lookup, so what it does when the bean is missing or ambiguous
 * decides whether the application starts with a default or fails.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
class BeanLookupTest {

    private BeanManager beanManager;

    @BeforeEach
    void setUp() {
        beanManager = Mockito.mock(BeanManager.class);
    }

    private void mockBean(Object reference, Class<?> type) {
        Bean bean = Mockito.mock(Bean.class);
        Mockito.when(beanManager.getBeans(Mockito.eq(type), Mockito.any()))
                .thenReturn(Set.of(bean));
        Mockito.when(beanManager.resolve(Mockito.any())).thenReturn(bean);
        CreationalContext context = Mockito.mock(CreationalContext.class);
        Mockito.when(beanManager.createCreationalContext(bean))
                .thenReturn(context);
        Mockito.when(beanManager.getReference(bean, type, context))
                .thenReturn(reference);
    }

    private void noBeans(Class<?> type) {
        Mockito.when(beanManager.getBeans(Mockito.eq(type), Mockito.any()))
                .thenReturn(Set.of());
    }

    @Test
    void lookup_beanResolves_returnsTheReference() {
        mockBean("resolved", String.class);

        assertEquals("resolved",
                new BeanLookup<>(beanManager, String.class).lookup());
    }

    @Test
    void lookup_noBean_answersNullAndTellsTheHandler() {
        noBeans(String.class);
        AtomicBoolean unsatisfied = new AtomicBoolean();

        assertNull(new BeanLookup<>(beanManager, String.class)
                .setUnsatisfiedHandler(() -> unsatisfied.set(true)).lookup());
        assertTrue(unsatisfied.get(),
                "the caller has to be able to log that the bean is absent");
    }

    @Test
    void lookup_beanManagerAnswersNull_isTreatedAsNoBean() {
        Mockito.when(
                beanManager.getBeans(Mockito.eq(String.class), Mockito.any()))
                .thenReturn(null);

        assertNull(new BeanLookup<>(beanManager, String.class).lookup());
    }

    @Test
    void lookupOrElseGet_noBean_usesTheFallback() {
        noBeans(String.class);

        assertEquals("fallback", new BeanLookup<>(beanManager, String.class)
                .lookupOrElseGet(() -> "fallback"));
    }

    @Test
    void lookup_ambiguous_throwsByDefault() {
        Bean bean = Mockito.mock(Bean.class);
        Mockito.when(
                beanManager.getBeans(Mockito.eq(String.class), Mockito.any()))
                .thenReturn(Set.of(bean));
        Mockito.when(beanManager.resolve(Mockito.any()))
                .thenThrow(new AmbiguousResolutionException("two of them"));

        // Nothing sensible can be chosen, so silence would hide a broken
        // application configuration.
        assertThrows(AmbiguousResolutionException.class,
                () -> new BeanLookup<>(beanManager, String.class).lookup());
    }

    @Test
    void lookupOrElseGet_ambiguousWithAHandler_fallsBackInstead() {
        Bean bean = Mockito.mock(Bean.class);
        Mockito.when(
                beanManager.getBeans(Mockito.eq(String.class), Mockito.any()))
                .thenReturn(Set.of(bean));
        AmbiguousResolutionException cause = new AmbiguousResolutionException(
                "two of them");
        Mockito.when(beanManager.resolve(Mockito.any())).thenThrow(cause);
        AtomicReference<AmbiguousResolutionException> reported = new AtomicReference<>();

        assertEquals("fallback",
                new BeanLookup<>(beanManager, String.class)
                        .setAmbiguousHandler(reported::set)
                        .lookupOrElseGet(() -> "fallback"));
        assertSame(cause, reported.get(),
                "a handler that swallows the failure still has to see it");
    }

    @Test
    void lookupAll_noBeans_isEmptyAndTellsTheHandler() {
        Mockito.when(beanManager.getBeans(Mockito.eq(String.class),
                Mockito.eq(VaadinServiceEnabled.Literal.INSTANCE)))
                .thenReturn(Set.of());
        AtomicBoolean unsatisfied = new AtomicBoolean();

        assertEquals(List.of(),
                new BeanLookup<>(beanManager, String.class,
                        VaadinServiceEnabled.Literal.INSTANCE)
                        .setUnsatisfiedHandler(() -> unsatisfied.set(true))
                        .lookupAll().toList());
        assertTrue(unsatisfied.get());
    }

    @Test
    void lookupAll_beansPresent_returnsEveryReference() {
        Bean bean = Mockito.mock(Bean.class);
        Mockito.when(beanManager.getBeans(Mockito.eq(String.class),
                Mockito.eq(VaadinServiceEnabled.Literal.INSTANCE)))
                .thenReturn(Set.of(bean));
        CreationalContext context = Mockito.mock(CreationalContext.class);
        Mockito.when(beanManager.createCreationalContext(bean))
                .thenReturn(context);
        Mockito.when(beanManager.getReference(bean, String.class, context))
                .thenReturn("resolved");
        AtomicBoolean unsatisfied = new AtomicBoolean();

        assertEquals(List.of("resolved"),
                new BeanLookup<>(beanManager, String.class,
                        VaadinServiceEnabled.Literal.INSTANCE)
                        .setUnsatisfiedHandler(() -> unsatisfied.set(true))
                        .lookupAll().toList());
        assertFalse(unsatisfied.get(),
                "the handler is for an empty result, not for a full one");
    }
}
