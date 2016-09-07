/*
 * Copyright 2015 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.spring.navigator;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.spring.access.ViewAccessControl;
import com.vaadin.spring.access.ViewInstanceAccessControl;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.internal.Conventions;
import com.vaadin.spring.internal.ViewCache;
import com.vaadin.spring.internal.ViewScopeImpl;
import com.vaadin.spring.server.SpringVaadinServletService;
import com.vaadin.ui.UI;

/**
 * A Vaadin {@link ViewProvider} that fetches the views from the Spring
 * application context. The views must implement the {@link View} interface and
 * be annotated with the {@link SpringView} annotation.
 * <p>
 * Use like this:
 *
 * <pre>
 * &#064;SpringUI
 * public class MyUI extends UI {
 *
 *     &#064;Autowired
 *     SpringViewProvider viewProvider;
 *
 *     protected void init(VaadinRequest vaadinRequest) {
 *         Navigator navigator = new Navigator(this, this);
 *         navigator.addProvider(viewProvider);
 *         setNavigator(navigator);
 *         // ...
 *     }
 * }
 * </pre>
 *
 * View-based security can be provided by creating a Spring bean that implements
 * the interface {@link com.vaadin.spring.access.ViewAccessControl} (for view
 * bean name and annotation based security) or
 * {@link com.vaadin.spring.access.ViewInstanceAccessControl} (if view instance
 * specific contextual data is needed). It is also possible to set an 'Access
 * Denied' view by using {@link #setAccessDeniedViewClass(Class)}.
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Henri Sara (hesara@vaadin.com)
 * @see SpringView
 */
public class SpringViewProvider implements ViewProvider {

    private static final long serialVersionUID = 6906237177564157222L;

    /*
     * Note! This is should be a singleton bean but it is probably not if you
     * serialize and deserialize a VaadinSession.
     * 
     * This should be fixed so that SpringViewProvider is not a singleton bean
     * but is UIScoped. This should also remove the need for using
     * UI.getCurrent().
     */

    // We can have multiple views with the same view name, as long as they
    // belong to different UI subclasses
    private final Map<String, Set<String>> viewNameToBeanNamesMap = new ConcurrentHashMap<String, Set<String>>();
    private transient BeanDefinitionRegistry beanDefinitionRegistry;
    private transient ApplicationContext applicationContext;
    private static final Logger LOGGER = LoggerFactory
            .getLogger(SpringViewProvider.class);

    private Class<? extends View> accessDeniedViewClass;

    @Autowired
    public SpringViewProvider(ApplicationContext applicationContext,
            BeanDefinitionRegistry beanDefinitionRegistry) {
        this.applicationContext = applicationContext;
        this.beanDefinitionRegistry = beanDefinitionRegistry;
    }

    /**
     * Returns the class of the access denied view. If set, a bean of this type
     * will be fetched from the application context and showed to the user when
     * a {@link com.vaadin.spring.access.ViewAccessControl} or a
     * {@link com.vaadin.spring.access.ViewInstanceAccessControl} denies access
     * to a view.
     *
     * @return the access denied view class, or {@code null} if not set.
     */
    public Class<? extends View> getAccessDeniedViewClass() {
        return accessDeniedViewClass;
    }

    /**
     * Sets the class of the access denied view. If set, a bean of this type
     * will be fetched from the application context and showed to the user when
     * a {@link com.vaadin.spring.access.ViewAccessControl} or a
     * {@link com.vaadin.spring.access.ViewInstanceAccessControl} denies access
     * to a view.
     *
     * @param accessDeniedViewClass
     *            the access denied view class, may be {@code null}.
     */
    public void setAccessDeniedViewClass(
            Class<? extends View> accessDeniedViewClass) {
        this.accessDeniedViewClass = accessDeniedViewClass;
    }

    @PostConstruct
    void init() {
        LOGGER.info("Looking up SpringViews");
        int count = 0;
        final String[] viewBeanNames = getWebApplicationContext()
                .getBeanNamesForAnnotation(SpringView.class);
        for (String beanName : viewBeanNames) {
            final Class<?> type = getWebApplicationContext().getType(beanName);
            if (View.class.isAssignableFrom(type)) {
                final SpringView annotation = getWebApplicationContext()
                        .findAnnotationOnBean(beanName, SpringView.class);
                final String viewName = getViewNameFromAnnotation(type,
                        annotation);
                LOGGER.debug("Found SpringView bean [{}] with view name [{}]",
                        beanName, viewName);
                if (getWebApplicationContext().isSingleton(beanName)) {
                    throw new IllegalStateException("SpringView bean ["
                            + beanName + "] must not be a singleton");
                }
                Set<String> beanNames = viewNameToBeanNamesMap.get(viewName);
                if (beanNames == null) {
                    beanNames = new ConcurrentSkipListSet<String>();
                    viewNameToBeanNamesMap.put(viewName, beanNames);
                }
                beanNames.add(beanName);
                count++;
            } else {
                LOGGER.error("The view bean [{}] does not implement View",
                        beanName);
                throw new IllegalStateException("SpringView bean [" + beanName
                        + "] must implement View");
            }
        }
        if (count == 0) {
            LOGGER.warn("No SpringViews found");
        } else if (count == 1) {
            LOGGER.info("1 SpringView found");
        } else {
            LOGGER.info("{} SpringViews found", count);
        }
    }

    protected String getViewNameFromAnnotation(Class<?> beanClass,
            SpringView annotation) {
        return Conventions.deriveMappingForView(beanClass, annotation);
    }

    @Override
    public String getViewName(String viewAndParameters) {
        LOGGER.trace("Extracting view name from [{}]", viewAndParameters);
        String viewName = null;
        if (isViewNameValidForCurrentUI(viewAndParameters)) {
            viewName = viewAndParameters;
        } else {
            int lastSlash = -1;
            String viewPart = viewAndParameters;
            while ((lastSlash = viewPart.lastIndexOf('/')) > -1) {
                viewPart = viewPart.substring(0, lastSlash);
                LOGGER.trace("Checking if [{}] is a valid view", viewPart);
                if (isViewNameValidForCurrentUI(viewPart)) {
                    viewName = viewPart;
                    break;
                }
            }
        }
        if (viewName == null) {
            LOGGER.trace("Found no view name in [{}]", viewAndParameters);
        } else {
            LOGGER.trace("[{}] is a valid view", viewName);
        }
        return viewName;
    }

    private boolean isViewNameValidForCurrentUI(String viewName) {
        final Set<String> beanNames = viewNameToBeanNamesMap.get(viewName);
        if (beanNames != null) {
            for (String beanName : beanNames) {
                if (isViewBeanNameValidForCurrentUI(beanName)) {
                    // if we have an access denied view, this is checked by
                    // getView()
                    return getAccessDeniedView() != null
                            || isAccessGrantedToBeanName(beanName);
                }
            }
        }
        return false;
    }

    private boolean isViewBeanNameValidForCurrentUI(String beanName) {
        try {
            final Class<?> type = getWebApplicationContext().getType(beanName);

            Assert.isAssignable(View.class, type,
                    "bean did not implement View interface");

            final UI currentUI = UI.getCurrent();
            final SpringView annotation = getWebApplicationContext()
                    .findAnnotationOnBean(beanName, SpringView.class);

            Assert.notNull(annotation,
                    "class did not have a SpringView annotation");

            if (annotation.ui().length == 0) {
                LOGGER.trace(
                        "View class [{}] with view name [{}] is available for all UI subclasses",
                        type.getCanonicalName(),
                        getViewNameFromAnnotation(type, annotation));
            } else {
                Class<? extends UI> validUI = getValidUIClass(currentUI,
                        annotation.ui());
                if (validUI != null) {
                    LOGGER.trace(
                            "View class [%s] with view name [{}] is available for UI subclass [{}]",
                            type.getCanonicalName(),
                            getViewNameFromAnnotation(type, annotation),
                            validUI.getCanonicalName());
                } else {
                    return false;
                }
            }

            return true;
        } catch (NoSuchBeanDefinitionException ex) {
            return false;
        }
    }

    private Class<? extends UI> getValidUIClass(UI currentUI,
            Class<? extends UI>[] validUIClasses) {
        for (Class<? extends UI> validUI : validUIClasses) {
            if (validUI.isAssignableFrom(currentUI.getClass())) {
                return validUI;
            }
        }
        return null;
    }

    @Override
    public View getView(String viewName) {
        final Set<String> beanNames = viewNameToBeanNamesMap.get(viewName);
        if (beanNames != null) {
            for (String beanName : beanNames) {
                if (isViewBeanNameValidForCurrentUI(beanName)) {
                    return getViewFromApplicationContext(viewName, beanName);
                }
            }
        }
        LOGGER.warn("Found no view with name [{}]", viewName);
        return null;
    }

    private View getViewFromApplicationContext(String viewName,
            String beanName) {
        View view = null;
        if (isAccessGrantedToBeanName(beanName)) {
            final BeanDefinition beanDefinition = getBeanDefinitionRegistry()
                    .getBeanDefinition(beanName);
            if (beanDefinition.getScope()
                    .equals(ViewScopeImpl.VAADIN_VIEW_SCOPE_NAME)) {
                LOGGER.trace("View [{}] is view scoped, activating scope",
                        viewName);
                final ViewCache viewCache = ViewScopeImpl
                        .getViewCacheRetrievalStrategy()
                        .getViewCache(getWebApplicationContext());
                viewCache.creatingView(viewName);
                try {
                    view = getViewFromApplicationContextAndCheckAccess(
                            beanName);
                } finally {
                    viewCache.viewCreated(viewName, view);
                }
            } else {
                view = getViewFromApplicationContextAndCheckAccess(beanName);
            }
        }
        if (view != null) {
            return view;
        } else {
            return getAccessDeniedView();
        }
    }

    private BeanDefinitionRegistry getBeanDefinitionRegistry() {
        if (beanDefinitionRegistry == null) {
            AutowireCapableBeanFactory factory = getWebApplicationContext()
                    .getAutowireCapableBeanFactory();
            beanDefinitionRegistry = (BeanDefinitionRegistry) factory;
        }
        return beanDefinitionRegistry;
    }

    private View getViewFromApplicationContextAndCheckAccess(String beanName) {
        final View view = (View) getWebApplicationContext().getBean(beanName);
        if (isAccessGrantedToViewInstance(beanName, view)) {
            return view;
        } else {
            return null;
        }
    }

    private View getAccessDeniedView() {
        if (accessDeniedViewClass != null) {
            return getWebApplicationContext().getBean(accessDeniedViewClass);
        } else {
            return null;
        }
    }

    private boolean isAccessGrantedToBeanName(String beanName) {
        final UI currentUI = UI.getCurrent();
        final Map<String, ViewAccessControl> accessDelegates = getWebApplicationContext()
                .getBeansOfType(ViewAccessControl.class);
        for (ViewAccessControl accessDelegate : accessDelegates.values()) {
            if (!accessDelegate.isAccessGranted(currentUI, beanName)) {
                LOGGER.debug(
                        "Access delegate [{}] denied access to view with bean name [{}]",
                        accessDelegate, beanName);
                return false;
            }
        }
        return true;
    }

    private boolean isAccessGrantedToViewInstance(String beanName, View view) {
        final UI currentUI = UI.getCurrent();
        final Map<String, ViewInstanceAccessControl> accessDelegates = getWebApplicationContext()
                .getBeansOfType(ViewInstanceAccessControl.class);
        for (ViewInstanceAccessControl accessDelegate : accessDelegates
                .values()) {
            if (!accessDelegate.isAccessGranted(currentUI, beanName, view)) {
                LOGGER.debug("Access delegate [{}] denied access to view [{}]",
                        accessDelegate, view);
                return false;
            }
        }
        return true;
    }

    protected ApplicationContext getWebApplicationContext() {
        if (applicationContext == null) {
            // Assume we have serialized and deserialized and Navigator is
            // trying to find a view so UI.getCurrent() is available
            UI ui = UI.getCurrent();
            if (ui == null) {
                throw new IllegalStateException(
                        "Could not find application context and no current UI is available");
            }
            applicationContext = ((SpringVaadinServletService) ui.getSession()
                    .getService()).getWebApplicationContext();
        }

        return applicationContext;
    }

}
