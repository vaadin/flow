/*
 * Copyright 2015-2017 The original authors
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
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
 * <p>
 * To specify a view to show when no matching view is found, use
 * {@link SpringNavigator#setErrorView(Class)}. Unlike version 1.0, version 1.1
 * does not use the access denied view always when no matching view is found.
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Henri Sara (hesara@vaadin.com)
 * @see SpringView
 */
public class SpringViewProvider implements ViewProvider {

    private static final long serialVersionUID = 6906237177564157222L;

    /**
     * Internal class used to communicate info on available views within the
     * view provider.
     */
    protected static class ViewInfo implements Serializable {
        private final String viewName;
        private final String beanName;

        public ViewInfo(String viewName, String beanName) {
            this.viewName = viewName;
            this.beanName = beanName;
        }

        public String getViewName() {
            return viewName;
        }

        public String getBeanName() {
            return beanName;
        }
    }

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
        LOGGER.debug("Looking up SpringViews");
        int count = 0;
        final String[] viewBeanNames = getWebApplicationContext()
                .getBeanNamesForAnnotation(SpringView.class);
        for (String beanName : viewBeanNames) {
            final Class<?> type = getWebApplicationContext().getType(beanName);
            if (View.class.isAssignableFrom(type)) {
                final SpringView annotation = AnnotatedElementUtils
                        .findMergedAnnotation(type, SpringView.class);
                final String viewName = getViewNameFromAnnotation(type,
                        annotation);
                LOGGER.debug("Found SpringView bean [{}] with view name [{}]",
                        beanName, viewName);
                if (getWebApplicationContext().isSingleton(beanName)) {
                    throw new IllegalStateException("SpringView bean ["
                            + beanName + "] must not be a singleton");
                }
                Set<String> beanNames = getViewNameToBeanNamesMap()
                        .get(viewName);
                if (beanNames == null) {
                    beanNames = new ConcurrentSkipListSet<String>();
                    getViewNameToBeanNamesMap().put(viewName, beanNames);
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
            LOGGER.debug("1 SpringView found");
        } else {
            LOGGER.debug("{} SpringViews found", count);
        }
    }

    protected String getViewNameFromAnnotation(Class<?> beanClass,
            SpringView annotation) {
        String viewName = Conventions.deriveMappingForView(beanClass, annotation);
        return getWebApplicationContext().getEnvironment().resolvePlaceholders(viewName);
    }

    /**
     * Return a collection with all the registered Spring views for the current
     * UI regardless of access restrictions.
     *
     * @return list of ViewInfo, not null
     */
    protected List<ViewInfo> getAllViewsForCurrentUI() {
        List<ViewInfo> views = new ArrayList<ViewInfo>();
        for (String viewName : getViewNameToBeanNamesMap().keySet()) {
            for (String beanName : getViewNameToBeanNamesMap().get(viewName)) {
                ViewInfo viewInfo = new ViewInfo(viewName, beanName);
                if (isViewValidForCurrentUI(viewInfo)) {
                    views.add(viewInfo);
                }
            }
        }
        return views;
    }

    /**
     * Return a collection with all the registered Spring views for the current
     * UI which the current user is allowed to access. Note that only view
     * type/bean level access is checked ({@link ViewAccessControl}), and view
     * instance specific checks ({@link ViewInstanceAccessControl}) are not
     * applied.
     *
     * @return list of ViewInfo, not null
     */
    protected List<ViewInfo> getAllowedViewsForCurrentUI() {
        List<ViewInfo> views = new ArrayList<ViewInfo>();
        for (ViewInfo view : getAllViewsForCurrentUI()) {
            if (isAccessGranted(view)) {
                views.add(view);
            }
        }
        return views;
    }

    /**
     * Return a collection with all the registered Spring views with the given
     * view name for the current UI and which the current user is allowed to
     * access. Note that only view type/bean level access is checked
     * ({@link ViewAccessControl}), and view instance specific checks
     * ({@link ViewInstanceAccessControl}) are not applied.
     *
     * @param viewName
     *            view name in the form returned by {@link #getViewName(String)}
     *            (no parameters)
     * @return list of ViewInfo, not null
     */
    protected List<ViewInfo> getAllowedViewsForCurrentUI(String viewName) {
        List<ViewInfo> views = new ArrayList<ViewInfo>();
        Set<String> allViews = getViewNameToBeanNamesMap().get(viewName);
        if (allViews != null) {
            for (String beanName : allViews) {
                ViewInfo viewInfo = new ViewInfo(viewName, beanName);
                if (isViewValidForCurrentUI(viewInfo)
                        && isAccessGranted(viewInfo)) {
                    views.add(viewInfo);
                }
            }
        }
        return views;
    }

    /**
     * Return a collection with all the registered Spring views for the current
     * UI which the current user is allowed to access. Note that only view
     * type/bean level access is checked ({@link ViewAccessControl}), and view
     * instance specific checks ({@link ViewInstanceAccessControl}) are not
     * applied.
     *
     * @return collection of view names, not null
     */
    public Collection<String> getViewNamesForCurrentUI() {
        Collection<String> viewNames = new HashSet<String>();
        for (ViewInfo view : getAllowedViewsForCurrentUI()) {
            viewNames.add(view.getViewName());
        }
        return viewNames;
    }

    @Override
    public String getViewName(String viewAndParameters) {
        LOGGER.trace("Extracting view name from [{}]", viewAndParameters);

        String viewName = getViewName(viewAndParameters,
                getAllowedViewsForCurrentUI());
        if (viewName != null) {
            return viewName;
        }

        // no views found
        LOGGER.trace("Found no view name in [{}]", viewAndParameters);

        // check also disallowed views to support access denied view
        if (getAccessDeniedViewClass() != null) {
            viewName = getViewName(viewAndParameters,
                    getAllViewsForCurrentUI());
            return viewName;
        }

        // nothing found, not even disallowed views
        return null;
    }

    protected String getViewName(String viewAndParameters,
            List<ViewInfo> views) {
        // first look for exact matches
        for (ViewInfo view : views) {
            if (view.getViewName().equals(viewAndParameters)) {
                LOGGER.trace("[{}] is a valid view", view.getViewName());
                return view.getViewName();
            }
        }

        // then look for prefix matches
        int lastSlash = -1;
        String viewPart = viewAndParameters;
        while ((lastSlash = viewPart.lastIndexOf('/')) > -1) {
            viewPart = viewPart.substring(0, lastSlash);
            LOGGER.trace("Checking if [{}] is a valid view", viewPart);
            for (ViewInfo view : views) {
                if (view.getViewName().equals(viewPart)) {
                    LOGGER.trace("[{}] is a valid view", view.getViewName());
                    return view.getViewName();
                }
            }
        }
        return null;
    }

    protected boolean isViewValidForCurrentUI(ViewInfo viewInfo) {
        String beanName = viewInfo.getBeanName();
        try {
            final Class<?> type = getWebApplicationContext().getType(beanName);

            Assert.isAssignable(View.class, type,
                    "bean did not implement View interface");

            final UI currentUI = UI.getCurrent();
            final SpringView annotation = AnnotatedElementUtils.findMergedAnnotation(type, SpringView.class);

            Assert.notNull(annotation,
                    "class did not have a SpringView annotation or an alias for it");

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
        List<ViewInfo> allowedViews = getAllowedViewsForCurrentUI(viewName);
        for (ViewInfo viewInfo : allowedViews) {
            View view = getViewFromApplicationContext(viewInfo);
            if (view != null) {
                return view;
            }
        }
        LOGGER.warn("Found no view with name [{}]", viewName);

        // Default to access denied view if defined -
        // returns null if no access denied view is set
        return getAccessDeniedView();
    }

    /**
     * Fetches a view from the application context. For view scoped views
     * created here, a view scope is set up to be active during the view
     * creation and until navigating away from the view.
     *
     * @param viewInfo
     *            view metadata
     * @return view instance from the application context, not null
     * @throws BeansException
     *             if no suitable bean is found or view scope initialization
     *             failed
     */
    protected View getViewFromApplicationContext(ViewInfo viewInfo) {
        View view = null;
        if (isAccessGranted(viewInfo)) {
            final BeanDefinition beanDefinition = getBeanDefinitionRegistry()
                    .getBeanDefinition(viewInfo.getBeanName());
            if (beanDefinition.getScope()
                    .equals(ViewScopeImpl.VAADIN_VIEW_SCOPE_NAME)) {
                LOGGER.trace("View [{}] is view scoped, activating scope",
                        viewInfo.getViewName());
                final ViewCache viewCache = ViewScopeImpl
                        .getViewCacheRetrievalStrategy()
                        .getViewCache(getWebApplicationContext());
                viewCache.creatingView(viewInfo.getViewName());
                try {
                    view = getViewFromApplicationContextAndCheckAccess(
                            viewInfo);
                } finally {
                    viewCache.viewCreated(viewInfo.getViewName(), view);
                }
            } else {
                // view scope is not active for non-view-scope views as we don't
                // hook into their lifecycle
                view = getViewFromApplicationContextAndCheckAccess(viewInfo);
            }
        }
        return view;
    }

    protected BeanDefinitionRegistry getBeanDefinitionRegistry() {
        if (beanDefinitionRegistry == null) {
            AutowireCapableBeanFactory factory = getWebApplicationContext()
                    .getAutowireCapableBeanFactory();
            beanDefinitionRegistry = (BeanDefinitionRegistry) factory;
        }
        return beanDefinitionRegistry;
    }

    protected View getViewFromApplicationContextAndCheckAccess(
            ViewInfo viewInfo) {
        String beanName = viewInfo.getBeanName();
        final View view = (View) getWebApplicationContext().getBean(beanName);
        if (isAccessGrantedToViewInstance(viewInfo, view)) {
            return view;
        } else {
            return null;
        }
    }

    /**
     * Returns an instance of the access denied view from the application
     * context based on {@link #setAccessDeniedViewClass(Class)}.
     *
     * @return access denied view instance from application context or null if
     *         no access denied view class is set
     */
    protected View getAccessDeniedView() {
        if (accessDeniedViewClass != null) {
            return getWebApplicationContext().getBean(accessDeniedViewClass);
        } else {
            return null;
        }
    }

    protected boolean isAccessGranted(ViewInfo view) {
        final UI currentUI = UI.getCurrent();
        final Map<String, ViewAccessControl> accessDelegates = getWebApplicationContext()
                .getBeansOfType(ViewAccessControl.class);
        for (ViewAccessControl accessDelegate : accessDelegates.values()) {
            if (!accessDelegate.isAccessGranted(currentUI,
                    view.getBeanName())) {
                LOGGER.debug(
                        "Access delegate [{}] denied access to view with bean name [{}]",
                        accessDelegate, view.getBeanName());
                return false;
            }
        }
        return true;
    }

    protected boolean isAccessGrantedToViewInstance(ViewInfo viewInfo,
            View view) {
        final UI currentUI = UI.getCurrent();
        final Map<String, ViewInstanceAccessControl> accessDelegates = getWebApplicationContext()
                .getBeansOfType(ViewInstanceAccessControl.class);
        for (ViewInstanceAccessControl accessDelegate : accessDelegates
                .values()) {
            if (!accessDelegate.isAccessGranted(currentUI,
                    viewInfo.getBeanName(), view)) {
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

    /**
     * Get the mapping from view names to the collections of corresponding bean
     * names. This method is primarily for internal use, and users should
     * typically override {@link #getAllViewsForCurrentUI()} or
     * {@link #getAllowedViewsForCurrentUI()} instead of using this method.
     *
     * @return internal mapping from view names to correspoding bean names
     */
    protected Map<String, Set<String>> getViewNameToBeanNamesMap() {
        return viewNameToBeanNamesMap;
    }

}
