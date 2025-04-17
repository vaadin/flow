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
package com.vaadin.flow.di;

import java.io.Serializable;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.server.DependencyFilter;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.MenuAccessControl;
import com.vaadin.flow.server.communication.IndexHtmlRequestListener;
import com.vaadin.flow.server.communication.UidlWriter;

/**
 * Delegate for discovering, creating and managing instances of various types
 * used by Flow. Dependency injection frameworks can provide an implementation
 * that manages instances according to the conventions of that framework.
 * <p>
 * {@link VaadinService} will by default use {@link ServiceLoader} for finding
 * an instantiator implementation. If not found {@link DefaultInstantiator} will
 * be used. It is possible to override this mechanism by overriding
 * {@link VaadinService#createInstantiator}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface Instantiator extends Serializable {

    /**
     * Gets all service init listeners to use. In addition to listeners defined
     * in some way native to a specific instantiator, it is also recommended to
     * support the default {@link ServiceLoader} convention. This can be done by
     * including the items from
     * {@link DefaultInstantiator#getServiceInitListeners()} in the returned
     * stream.
     *
     * @return stream of service init listeners, not <code>null</code>
     */
    Stream<VaadinServiceInitListener> getServiceInitListeners();

    /**
     * Processes the available Index HTML request listeners. This method can
     * supplement the set of Index HTML request listeners provided by
     * {@link VaadinServiceInitListener} implementations.
     * <p>
     * The default implementation returns the original listeners without
     * changes.
     *
     * @param indexHtmlRequestListeners
     *            a stream of Index HTML request listeners provided by service
     *            init listeners, not <code>null</code>
     *
     * @return a stream of all Index HTML request listeners to use, not
     *         <code>null</code>
     */
    default Stream<IndexHtmlRequestListener> getIndexHtmlRequestListeners(
            Stream<IndexHtmlRequestListener> indexHtmlRequestListeners) {
        return indexHtmlRequestListeners;
    }

    /**
     * Processes the available dependency filters. This method can supplement
     * the set of dependency filters provided by
     * {@link VaadinServiceInitListener} implementations.
     * <p>
     * The default implementation returns the original handlers without changes.
     * <p>
     * The order of the filters inside the stream defines the order of the
     * execution of those listeners by the
     * {@link UidlWriter#createUidl(UI, boolean)} method.
     *
     * @param serviceInitFilters
     *            a stream of dependency filters provided by service init
     *            listeners, not <code>null</code>
     *
     * @return a stream of all dependency filters to use, not <code>null</code>
     */
    default Stream<DependencyFilter> getDependencyFilters(
            Stream<DependencyFilter> serviceInitFilters) {
        return serviceInitFilters;
    }

    /**
     * Provides an instance of any given type, this is an abstraction that
     * allows to make use of DI-frameworks from add-ons.
     * <p>
     * How the object is created and whether it is being cached or not is up to
     * the implementation.
     *
     * @param type
     *            the instance type to create, not <code>null</code>
     * @param <T>
     *            the type of the instance to create
     *
     * @return an instance of the given type
     */
    <T> T getOrCreate(Class<T> type);

    /**
     * Return the application-defined class for the given instance: usually
     * simply the class of the given instance, but the original class in case of
     * a runtime generated subclass.
     *
     * @param instance
     *            the instance to check
     * @return the user-defined class
     */
    default Class<?> getApplicationClass(Object instance) {
        Objects.requireNonNull(instance, "Instance cannot be null");
        return getApplicationClass(instance.getClass());
    }

    /**
     * Return the application-defined class for the given class: usually simply
     * the given class, but the original class in case of a runtime generated
     * subclass.
     *
     * @param clazz
     *            the class to check
     * @return the user-defined class
     */
    default Class<?> getApplicationClass(Class<?> clazz) {
        Class<?> appClass = clazz;
        while (appClass != null && appClass != Object.class
                && appClass.isSynthetic()) {
            appClass = appClass.getSuperclass();
        }
        return appClass;
    }

    /**
     * Creates an instance of a navigation target or router layout. This method
     * is not called in cases when a component instance is reused when
     * navigating.
     *
     * @param routeTargetType
     *            the instance type to create, not <code>null</code>
     * @param event
     *            the navigation event for which the instance is created, not
     *            <code>null</code>
     * @param <T>
     *            the route target type
     *
     * @return the created instance, not <code>null</code>
     */
    default <T extends HasElement> T createRouteTarget(Class<T> routeTargetType,
            NavigationEvent event) {
        return getOrCreate(routeTargetType);
    }

    /**
     * Creates an instance of a component by its {@code componentClass}.
     *
     * @param componentClass
     *            the instance type to create, not <code>null</code>
     * @param <T>
     *            the component type
     *
     * @return the created instance, not <code>null</code>
     */
    <T extends Component> T createComponent(Class<T> componentClass);

    /**
     * Gets the instantiator to use for the given UI.
     *
     * @param ui
     *            the attached UI for which to find an instantiator, not
     *            <code>null</code>
     * @return the instantiator, not <code>null</code>
     */
    static Instantiator get(UI ui) {
        assert ui != null;

        VaadinSession session = ui.getSession();
        assert session != null;

        return session.getService().getInstantiator();
    }

    /**
     * Get the I18NProvider if one has been defined.
     *
     * @return I18NProvider instance
     */
    default I18NProvider getI18NProvider() {
        return getOrCreate(I18NProvider.class);
    }

    /**
     * Get the MenuAccessControl.
     *
     * @return MenuAccessControl instance
     */
    default MenuAccessControl getMenuAccessControl() {
        return getOrCreate(MenuAccessControl.class);
    }
}
