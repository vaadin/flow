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
package com.vaadin.quarkus.context;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.InjectionTarget;

import com.vaadin.quarkus.AnyLiteral;

/**
 * A modified copy of org.apache.deltaspike.core.api.provider.BeanProvider.
 *
 * This class contains utility methods for resolution of contextual references
 * in situations where no injection is available because the current class is
 * not managed by the CDI Container. This can happen in e.g. a JPA 2.0
 * EntityListener, a ServletFilter, a Spring managed Bean, etc.
 *
 * <p>
 * <b>Attention:</b> This approach is intended for use in user code at runtime.
 * If BeanProvider is used during Container boot (in an Extension), non-portable
 * behaviour results. The CDI specification only allows injection of the
 * BeanManager during CDI container boot time.
 * </p>
 *
 */
@Typed()
public final class BeanProvider {
    private static final Logger LOG = Logger
            .getLogger(BeanProvider.class.getName());

    private BeanProvider() {
        // this is a utility class which doesn't get instantiated.
    }

    /**
     * Get a Contextual Reference by its type and qualifiers. You can use this
     * method to get contextual references of a given type. A "Contextual
     * Reference" is a proxy which will automatically resolve the correct
     * contextual instance when you access any method.
     *
     * <p>
     * <b>Attention:</b> You shall not use this method to manually resolve a
     * &#064;Dependent bean! The reason is that contextual instances usually
     * live in the well-defined lifecycle of their injection point (the bean
     * they got injected into). But if we manually resolve a &#064;Dependent
     * bean, then it does <b>not</b> belong to such well defined lifecycle
     * (because &#064;Dependent is not &#064;NormalScoped) and thus will not be
     * automatically destroyed at the end of the lifecycle. You need to manually
     * destroy this contextual instance via
     * {@link jakarta.enterprise.context.spi.Contextual#destroy(Object, jakarta.enterprise.context.spi.CreationalContext)}.
     * Thus you also need to manually store the CreationalContext and the Bean
     * you used to create the contextual instance.
     * </p>
     *
     * @param type
     *            the type of the bean in question
     * @param qualifiers
     *            additional qualifiers which further distinct the resolved bean
     * @param <T>
     *            target type
     *
     * @return the resolved Contextual Reference
     *
     * @throws IllegalStateException
     *             if the bean could not be found.
     * @see #getContextualReference(Class, boolean, Annotation...)
     */
    public static <T> T getContextualReference(Class<T> type,
            Annotation... qualifiers) {
        return getContextualReference(type, false, qualifiers);
    }

    /**
     * {@link #getContextualReference(Class, Annotation...)} which returns
     * <code>null</code> if the 'optional' parameter is set to
     * <code>true</code>.
     *
     * @param type
     *            the type of the bean in question
     * @param optional
     *            if <code>true</code> it will return <code>null</code> if no
     *            bean could be found or created. Otherwise it will throw an
     *            {@code IllegalStateException}
     * @param qualifiers
     *            additional qualifiers which distinguish the resolved bean
     * @param <T>
     *            target type
     *
     * @return the resolved Contextual Reference
     *
     * @see #getContextualReference(Class, Annotation...)
     */
    public static <T> T getContextualReference(Class<T> type, boolean optional,
            Annotation... qualifiers) {
        BeanManager beanManager = getBeanManager();

        return getContextualReference(beanManager, type, optional, qualifiers);
    }

    /**
     * {@link #getContextualReference(Class, Annotation...)} which returns
     * <code>null</code> if the 'optional' parameter is set to
     * <code>true</code>. This method is intended for usage where the BeanManger
     * is known, e.g. in Extensions.
     *
     * @param beanManager
     *            the BeanManager to use
     * @param type
     *            the type of the bean in question
     * @param optional
     *            if <code>true</code> it will return <code>null</code> if no
     *            bean could be found or created. Otherwise it will throw an
     *            {@code IllegalStateException}
     * @param qualifiers
     *            additional qualifiers which further distinct the resolved bean
     * @param <T>
     *            target type
     *
     * @return the resolved Contextual Reference
     *
     * @see #getContextualReference(Class, Annotation...)
     */
    public static <T> T getContextualReference(BeanManager beanManager,
            Class<T> type, boolean optional, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);

        if (beans == null || beans.isEmpty()) {
            if (optional) {
                return null;
            }

            throw new IllegalStateException("Could not find beans for Type="
                    + type + " and qualifiers:" + Arrays.toString(qualifiers));
        }

        return getContextualReference(type, beanManager, beans);
    }

    /**
     * Get a Contextual Reference by its EL Name. This only works for beans with
     * the &#064;Named annotation.
     *
     * <p>
     * <b>Attention:</b> please see the notes on manually resolving
     * &#064;Dependent beans in
     * {@link #getContextualReference(Class, java.lang.annotation.Annotation...)}!
     * </p>
     *
     * @param name
     *            the EL name of the bean
     *
     * @return the resolved Contextual Reference
     *
     * @throws IllegalStateException
     *             if the bean could not be found.
     * @see #getContextualReference(String, boolean)
     */
    public static Object getContextualReference(String name) {
        return getContextualReference(name, false);
    }

    /**
     * Get a Contextual Reference by its EL Name. This only works for beans with
     * the &#064;Named annotation.
     *
     * <p>
     * <b>Attention:</b> please see the notes on manually resolving
     * &#064;Dependent beans in
     * {@link #getContextualReference(Class, java.lang.annotation.Annotation...)}!
     * </p>
     *
     * @param name
     *            the EL name of the bean
     * @param optional
     *            if <code>true</code> it will return <code>null</code> if no
     *            bean could be found or created. Otherwise it will throw an
     *            {@code IllegalStateException}
     *
     * @return the resolved Contextual Reference
     */
    public static Object getContextualReference(String name, boolean optional) {
        return getContextualReference(name, optional, Object.class);
    }

    /**
     * Get a Contextual Reference by its EL Name. This only works for beans with
     * the &#064;Named annotation.
     *
     * <p>
     * <b>Attention:</b> please see the notes on manually resolving
     * &#064;Dependent beans in
     * {@link #getContextualReference(Class, java.lang.annotation.Annotation...)}!
     * </p>
     *
     * @param name
     *            the EL name of the bean
     * @param optional
     *            if <code>true</code> it will return <code>null</code> if no
     *            bean could be found or created. Otherwise it will throw an
     *            {@code IllegalStateException}
     * @param type
     *            the type of the bean in question - use
     *            {@link #getContextualReference(String, boolean)} if the type
     *            is unknown e.g. in dyn. use-cases
     * @param <T>
     *            target type
     *
     * @return the resolved Contextual Reference
     */
    public static <T> T getContextualReference(String name, boolean optional,
            Class<T> type) {
        return getContextualReference(getBeanManager(), name, optional, type);
    }

    /**
     * <p>
     * Get a Contextual Reference by its EL Name. This only works for beans with
     * the &#064;Named annotation.
     * </p>
     *
     * <p>
     * <b>Attention:</b> please see the notes on manually resolving
     * &#064;Dependent bean in
     * {@link #getContextualReference(Class, boolean, java.lang.annotation.Annotation...)}!
     * </p>
     *
     *
     * @param beanManager
     *            the BeanManager to use
     * @param name
     *            the EL name of the bean
     * @param optional
     *            if <code>true</code> it will return <code>null</code> if no
     *            bean could be found or created. Otherwise it will throw an
     *            {@code IllegalStateException}
     * @param type
     *            the type of the bean in question - use
     *            {@link #getContextualReference(String, boolean)} if the type
     *            is unknown e.g. in dyn. use-cases
     * @param <T>
     *            target type
     * @return the resolved Contextual Reference
     */
    public static <T> T getContextualReference(BeanManager beanManager,
            String name, boolean optional, Class<T> type) {
        Set<Bean<?>> beans = beanManager.getBeans(name);

        if (beans == null || beans.isEmpty()) {
            if (optional) {
                return null;
            }

            throw new IllegalStateException("Could not find beans for Type="
                    + type + " and name:" + name);
        }

        return getContextualReference(type, beanManager, beans);
    }

    /**
     * Get the Contextual Reference for the given bean.
     *
     * <p>
     * <b>Attention:</b> please see the notes on manually resolving
     * &#064;Dependent beans in
     * {@link #getContextualReference(Class, java.lang.annotation.Annotation...)}!
     * </p>
     *
     * @param type
     *            the type of the bean in question
     * @param bean
     *            bean definition for the contextual reference
     * @param <T>
     *            target type
     *
     * @return the resolved Contextual Reference
     */
    public static <T> T getContextualReference(Class<T> type, Bean<T> bean) {
        return getContextualReference(type, getBeanManager(), bean);
    }

    private static <T> T getContextualReference(Class<T> type,
            BeanManager beanManager, Bean<?> bean) {
        // noinspection unchecked
        return getContextualReference(type, beanManager,
                Collections.<Bean<?>> singleton(bean));
    }

    /**
     * Get a list of Contextual References by type, regardless of qualifiers
     * (including dependent scoped beans).
     *
     * You can use this method to get all contextual references of a given type.
     * A 'Contextual Reference' is a proxy which will automatically resolve the
     * correct contextual instance when you access any method.
     *
     * <p>
     * <b>Attention:</b> please see the notes on manually resolving
     * &#064;Dependent beans in
     * {@link #getContextualReference(Class, java.lang.annotation.Annotation...)}!
     * </p>
     * <p>
     * <b>Attention:</b> This will also return instances of beans for which an
     * Alternative exists! The &#064;Alternative resolving is only done via
     * {@link BeanManager#resolve(java.util.Set)} which we cannot use in this
     * case!
     * </p>
     *
     * @param type
     *            the type of the bean in question
     * @param optional
     *            if <code>true</code> it will return an empty list if no bean
     *            could be found or created. Otherwise it will throw an
     *            {@code IllegalStateException}
     * @param <T>
     *            target type
     *
     * @return the resolved list of Contextual Reference or an empty-list if
     *         optional is true
     */
    public static <T> List<T> getContextualReferences(Class<T> type,
            boolean optional) {
        return getContextualReferences(type, optional, true);
    }

    /**
     * Get a list of Contextual References by type, regardless of the qualifier.
     *
     * Further details are available at
     * {@link #getContextualReferences(Class, boolean)}.
     * <p>
     * <b>Attention:</b> please see the notes on manually resolving
     * &#064;Dependent bean in
     * {@link #getContextualReference(Class, java.lang.annotation.Annotation...)}!
     * </p>
     * <p>
     * <b>Attention:</b> This will also return instances of beans for which an
     * Alternative exists! The &#064;Alternative resolving is only done via
     * {@link BeanManager#resolve(java.util.Set)} which we cannot use in this
     * case!
     * </p>
     *
     * @param type
     *            the type of the bean in question
     * @param optional
     *            if <code>true</code> it will return an empty list if no bean
     *            could be found or created. Otherwise it will throw an
     *            {@code IllegalStateException}
     * @param includeDefaultScopedBeans
     *            specifies if dependent scoped beans should be included in the
     *            result
     * @param <T>
     *            target type
     *
     * @return the resolved list of Contextual Reference or an empty-list if
     *         optional is true
     */
    public static <T> List<T> getContextualReferences(Class<T> type,
            boolean optional, boolean includeDefaultScopedBeans) {
        BeanManager beanManager = getBeanManager();

        Set<Bean<T>> beans = getBeanDefinitions(type, optional,
                includeDefaultScopedBeans, beanManager);

        List<T> result = new ArrayList<T>(beans.size());

        for (Bean<?> bean : beans) {
            // noinspection unchecked
            result.add(getContextualReference(type, beanManager, bean));
        }
        return result;
    }

    /**
     * Get a set of {@link Bean} definitions by type, regardless of qualifiers.
     *
     * @param type
     *            the type of the bean in question
     * @param optional
     *            if <code>true</code> it will return an empty set if no bean
     *            could be found. Otherwise it will throw an
     *            {@code IllegalStateException}
     * @param includeDefaultScopedBeans
     *            specifies whether dependent scoped beans should be included in
     *            the result
     * @param <T>
     *            target type
     *
     * @return the resolved set of {@link Bean} definitions or an empty set if
     *         optional is true
     */
    public static <T> Set<Bean<T>> getBeanDefinitions(Class<T> type,
            boolean optional, boolean includeDefaultScopedBeans) {
        BeanManager beanManager = getBeanManager();

        return getBeanDefinitions(type, optional, includeDefaultScopedBeans,
                beanManager);
    }

    /**
     * Get a set of {@link Bean} definitions by type, regardless of qualifiers.
     *
     * @param type
     *            the type of the bean in question
     * @param optional
     *            if <code>true</code> it will return an empty set if no bean
     *            could be found. Otherwise it will throw an
     *            {@code IllegalStateException}
     * @param includeDefaultScopedBeans
     *            specifies whether dependent scoped beans should be included in
     *            the result
     * @param <T>
     *            target type
     * @param beanManager
     *            the {@link BeanManager} to use
     *
     * @return the resolved set of {@link Bean} definitions or an empty set if
     *         optional is true
     */
    public static <T> Set<Bean<T>> getBeanDefinitions(Class<T> type,
            boolean optional, boolean includeDefaultScopedBeans,
            BeanManager beanManager) {
        Set<Bean<?>> beans = beanManager.getBeans(type, new AnyLiteral());

        if (beans == null || beans.isEmpty()) {
            if (optional) {
                return Collections.emptySet();
            }

            throw new IllegalStateException(
                    "Could not find beans for Type=" + type);
        }

        if (!includeDefaultScopedBeans) {
            beans = filterDefaultScopedBeans(beans);
        }

        Set<Bean<T>> result = new HashSet<Bean<T>>();

        for (Bean<?> bean : beans) {
            // noinspection unchecked
            @SuppressWarnings("unchecked")
            Bean<T> beanT = (Bean<T>) bean;
            result.add(beanT);
        }

        return result;
    }

    /**
     * Performs dependency injection on an instance. Useful for instances which
     * aren't managed by CDI.
     * <p>
     * <b>Attention:</b> <br>
     * The resulting instance isn't managed by CDI; only fields annotated
     * with @Inject get initialized.
     *
     * @param instance
     *            current instance
     * @param <T>
     *            current type
     *
     * @return instance with injected fields (if possible - or null if the given
     *         instance is null)
     */
    @SuppressWarnings("unchecked")
    public static <T> T injectFields(T instance) {
        if (instance == null) {
            return null;
        }

        BeanManager beanManager = getBeanManager();

        CreationalContext<T> creationalContext = beanManager
                .createCreationalContext(null);

        AnnotatedType<T> annotatedType = beanManager
                .createAnnotatedType((Class<T>) instance.getClass());
        InjectionTarget<T> injectionTarget = beanManager
                .getInjectionTargetFactory(annotatedType)
                .createInjectionTarget(null);
        injectionTarget.inject(instance, creationalContext);
        return instance;
    }

    private static Set<Bean<?>> filterDefaultScopedBeans(Set<Bean<?>> beans) {
        Set<Bean<?>> result = new HashSet<Bean<?>>(beans.size());

        for (Bean<?> currentBean : beans) {
            if (!Dependent.class.isAssignableFrom(currentBean.getScope())) {
                result.add(currentBean);
            }
        }
        return result;
    }

    /**
     * Internal helper method to resolve the right bean and resolve the
     * contextual reference.
     *
     * @param type
     *            the type of the bean in question
     * @param beanManager
     *            current bean-manager
     * @param beans
     *            beans in question
     * @param <T>
     *            target type
     * @return the contextual reference
     */
    private static <T> T getContextualReference(Class<T> type,
            BeanManager beanManager, Set<Bean<?>> beans) {
        Bean<?> bean = beanManager.resolve(beans);

        CreationalContext<?> creationalContext = beanManager
                .createCreationalContext(bean);

        @SuppressWarnings({ "unchecked", "UnnecessaryLocalVariable" })
        T result = (T) beanManager.getReference(bean, type, creationalContext);
        return result;
    }

    private static BeanManager getBeanManager() {
        return CDI.current().getBeanManager();
    }
}
