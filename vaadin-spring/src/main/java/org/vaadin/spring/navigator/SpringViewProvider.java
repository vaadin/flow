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
package org.vaadin.spring.navigator;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.ui.UI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.vaadin.spring.navigator.annotation.VaadinView;

import javax.annotation.PostConstruct;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * A Vaadin {@link ViewProvider} that fetches the views from the Spring application context. The views
 * must implement the {@link View} interface and be annotated with the {@link VaadinView} annotation.
 * <p/>
 * Use like this:
 * <pre>
 *         &#64;VaadinUI
 *         public class MyUI extends UI {
 *
 *              &#64;Autowired SpringViewProvider viewProvider;
 *
 *              protected void init(VaadinRequest vaadinRequest) {
 *                  Navigator navigator = new Navigator(this, this);
 *                  navigator.addProvider(viewProvider);
 *                  setNavigator(navigator);
 *                  // ...
 *              }
 *         }
 *     </pre>
 *
 * View-based security can be provided by creating a Spring bean that implements the {@link org.vaadin.spring.navigator.SpringViewProvider.ViewProviderAccessDelegate} interface.
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @see VaadinView
 */
public class SpringViewProvider implements ViewProvider {

    private static final long serialVersionUID = 6906237177564157222L;
    
    /*
     * Note! This is a singleton bean!
     */

    // We can have multiple views with the same view name, as long as they belong to different UI subclasses
    private final Map<String, Set<String>> viewNameToBeanNamesMap = new ConcurrentHashMap<String, Set<String>>();
    private final ApplicationContext applicationContext;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    public SpringViewProvider(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    void init() {
        logger.info("Looking up VaadinViews");
        int count = 0;
        final String[] viewBeanNames = applicationContext.getBeanNamesForAnnotation(VaadinView.class);
        for (String beanName : viewBeanNames) {
            final Class<?> type = applicationContext.getType(beanName);
            if (View.class.isAssignableFrom(type)) {
                final VaadinView annotation = applicationContext.findAnnotationOnBean(beanName, VaadinView.class);
                final String viewName = annotation.name();
                logger.debug("Found VaadinView bean [{}] with view name [{}]", beanName, viewName);
                if (applicationContext.isSingleton(beanName)) {
                    throw new IllegalStateException("VaadinView bean [" + beanName + "] must not be a singleton");
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
            logger.warn("No VaadinViews found");
        } else if (count == 1) {
            logger.info("1 VaadinView found");
        } else {
            logger.info("{} VaadinViews found", count);
        }
    }

    @Override
    public String getViewName(String viewAndParameters) {
        logger.trace("Extracting view name from [{}]", viewAndParameters);
        String viewName = null;
        if (isViewNameValidForCurrentUI(viewAndParameters)) {
            viewName = viewAndParameters;
        } else {
            int lastSlash = -1;
            String viewPart = viewAndParameters;
            while ((lastSlash = viewPart.lastIndexOf('/')) > -1) {
                viewPart = viewPart.substring(0, lastSlash);
                logger.trace("Checking if [{}] is a valid view", viewPart);
                if (isViewNameValidForCurrentUI(viewPart)) {
                    viewName = viewPart;
                    break;
                }
            }
        }
        if (viewName == null) {
            logger.trace("Found no view name in [{}]", viewAndParameters);
        } else {
            logger.trace("[{}] is a valid view", viewName);
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

            Assert.isAssignable(View.class, type, "bean did not implement View interface");

            final UI currentUI = UI.getCurrent();
            final VaadinView annotation = applicationContext.findAnnotationOnBean(beanName, VaadinView.class);

            Assert.notNull(annotation, "class did not have a VaadinView annotation");

            final Map<String, ViewProviderAccessDelegate> accessDelegates = applicationContext.getBeansOfType(ViewProviderAccessDelegate.class);
            for (ViewProviderAccessDelegate accessDelegate : accessDelegates.values()) {
                if (!accessDelegate.isAccessGranted(beanName, currentUI)) {
                    logger.debug("Access delegate [{}] denied access to view class [{}]", accessDelegate, type.getCanonicalName());
                    return false;
                }
            }

            if (annotation.ui().length == 0) {
                logger.trace("View class [{}] with view name [{}] is available for all UI subclasses", type.getCanonicalName(), annotation.name());
                return true;
            } else {
                for (Class<? extends UI> validUI : annotation.ui()) {
                    if (validUI == currentUI.getClass()) {
                        logger.trace("View class [%s] with view name [{}] is available for UI subclass [{}]", type.getCanonicalName(), annotation.name(), validUI.getCanonicalName());
                        return true;
                    }
                }
            }
            return false;
        } catch (NoSuchBeanDefinitionException ex) {
            return false;
        }
    }

    @Override
    public View getView(String viewName) {
        final Set<String> beanNames = viewNameToBeanNamesMap.get(viewName);
        if (beanNames != null) {
            for (String beanName : beanNames) {
                if (isViewBeanNameValidForCurrentUI(beanName)) {
                    View view = (View) applicationContext.getBean(beanName);
                    if (isAccessGrantedToViewInstance(view)) {
                        return view;
                    }
                }
            }
        }
        logger.warn("Found no view with name [{}]", viewName);
        return null;
    }

    private boolean isAccessGrantedToViewInstance(View view) {
        final UI currentUI = UI.getCurrent();
        final Map<String, ViewProviderAccessDelegate> accessDelegates = applicationContext.getBeansOfType(ViewProviderAccessDelegate.class);
        for (ViewProviderAccessDelegate accessDelegate : accessDelegates.values()) {
            if (!accessDelegate.isAccessGranted(view, currentUI)) {
                logger.debug("Access delegate [{}] denied access to view [{}]", accessDelegate, view);
                return false;
            }
        }
        return true;
    }

    /**
     * Interface to be implemented by Spring beans that will be consulted before the Spring View provider
     * provides a view. If any of the view providers deny access, the view provider will act like no such
     * view ever existed.
     */
    public interface ViewProviderAccessDelegate {

        /**
         * Checks if the current user has access to the specified view and UI.
         *
         * @param beanName the bean name of the view, never {@code null}.
         * @param ui       the UI, never {@code null}.
         * @return true if access is granted, false if access is denied.
         */
        boolean isAccessGranted(String beanName, UI ui);

        /**
         * Checks if the current user has access to the specified view instance and UI. This method is invoked
         * after {@link #isAccessGranted(com.vaadin.navigator.View, com.vaadin.ui.UI)}, when the view instance
         * has already been created, but before it has been returned by the view provider.
         *
         * @param view the view instance, never {@code null}.
         * @param ui   the UI, never {@code null}.
         * @return true if access is granted, false if access is denied.
         */
        boolean isAccessGranted(View view, UI ui);
    }
}
