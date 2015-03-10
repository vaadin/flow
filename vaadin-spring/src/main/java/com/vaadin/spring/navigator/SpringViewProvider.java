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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.internal.Conventions;
import com.vaadin.spring.internal.VaadinViewScope;
import com.vaadin.spring.internal.ViewCache;
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
 * the {@link com.vaadin.spring.navigator.ViewProviderAccessDelegate} interface.
 * It is also possible to set an 'Access Denied' view by using
 * {@link #setAccessDeniedViewClass(Class)}.
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Henri Sara (hesara@vaadin.com)
 * @see SpringView
 */
public class SpringViewProvider implements ViewProvider {

    private static final long serialVersionUID = 6906237177564157222L;

    /*
     * Note! This is a singleton bean!
     */

    // We can have multiple views with the same view name, as long as they
    // belong to different UI subclasses
    private final Map<String, Set<String>> viewNameToBeanNamesMap = new ConcurrentHashMap<String, Set<String>>();
    private final ApplicationContext applicationContext;
    private final BeanDefinitionRegistry beanDefinitionRegistry;
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
     * a {@link com.vaadin.spring.navigator.ViewProviderAccessDelegate} denies
     * access to a view.
     *
     * @return the access denied view class, or {@code null} if not set.
     */
    public Class<? extends View> getAccessDeniedViewClass() {
        return accessDeniedViewClass;
    }

    /**
     * Sets the class of the access denied view. If set, a bean of this type
     * will be fetched from the application context and showed to the user when
     * a {@link com.vaadin.spring.navigator.ViewProviderAccessDelegate} denies
     * access to a view.
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
        final String[] viewBeanNames = applicationContext
                .getBeanNamesForAnnotation(SpringView.class);
        for (String beanName : viewBeanNames) {
            final Class<?> type = applicationContext.getType(beanName);
            if (View.class.isAssignableFrom(type)) {
                final SpringView annotation = applicationContext
                        .findAnnotationOnBean(beanName, SpringView.class);
                final String viewName = getViewNameFromAnnotation(type,
                        annotation);
                LOGGER.debug("Found SpringView bean [{}] with view name [{}]",
                        beanName, viewName);
                if (applicationContext.isSingleton(beanName)) {
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
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isViewBeanNameValidForCurrentUI(String beanName) {
        try {
            final Class<?> type = applicationContext.getType(beanName);

            Assert.isAssignable(View.class, type,
                    "bean did not implement View interface");

            final UI currentUI = UI.getCurrent();
            final SpringView annotation = applicationContext
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

            final Map<String, ViewProviderAccessDelegate> accessDelegates = applicationContext
                    .getBeansOfType(ViewProviderAccessDelegate.class);
            for (ViewProviderAccessDelegate accessDelegate : accessDelegates
                    .values()) {
                if (!accessDelegate.isAccessGranted(currentUI, beanName)) {
                    LOGGER.debug(
                            "Access delegate [{}] denied access to view class [{}]",
                            accessDelegate, type.getCanonicalName());
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

    private View getViewFromApplicationContext(String viewName, String beanName) {
        View view = null;
        if (isAccessGrantedToBeanName(beanName)) {
            final BeanDefinition beanDefinition = beanDefinitionRegistry
                    .getBeanDefinition(beanName);
            if (beanDefinition.getScope().equals(
                    VaadinViewScope.VAADIN_VIEW_SCOPE_NAME)) {
                LOGGER.trace("View [{}] is view scoped, activating scope",
                        viewName);
                final ViewCache viewCache = VaadinViewScope
                        .getViewCacheRetrievalStrategy().getViewCache(
                                applicationContext);
                viewCache.creatingView(viewName);
                try {
                    view = getViewFromApplicationContextAndCheckAccess(beanName);
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

    private View getViewFromApplicationContextAndCheckAccess(String beanName) {
        final View view = (View) applicationContext.getBean(beanName);
        if (isAccessGrantedToViewInstance(beanName, view)) {
            return view;
        } else {
            return null;
        }
    }

    private View getAccessDeniedView() {
        if (accessDeniedViewClass != null) {
            return applicationContext.getBean(accessDeniedViewClass);
        } else {
            return null;
        }
    }

    private boolean isAccessGrantedToBeanName(String beanName) {
        final UI currentUI = UI.getCurrent();
        final Map<String, ViewProviderAccessDelegate> accessDelegates = applicationContext
                .getBeansOfType(ViewProviderAccessDelegate.class);
        for (ViewProviderAccessDelegate accessDelegate : accessDelegates
                .values()) {
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
        final Map<String, ViewProviderAccessDelegate> accessDelegates = applicationContext
                .getBeansOfType(ViewProviderAccessDelegate.class);
        for (ViewProviderAccessDelegate accessDelegate : accessDelegates
                .values()) {
            if (!accessDelegate.isAccessGranted(currentUI, beanName, view)) {
                LOGGER.debug("Access delegate [{}] denied access to view [{}]",
                        accessDelegate, view);
                return false;
            }
        }
        return true;
    }
}
