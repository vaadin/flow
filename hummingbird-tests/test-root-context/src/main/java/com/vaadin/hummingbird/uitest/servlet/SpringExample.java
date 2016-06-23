/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.servlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.router.HasChildView;
import com.vaadin.hummingbird.router.NavigationEvent;
import com.vaadin.hummingbird.router.RouterConfiguration;
import com.vaadin.hummingbird.router.RouterConfigurator;
import com.vaadin.hummingbird.router.View;
import com.vaadin.hummingbird.router.ViewRenderer;
import com.vaadin.hummingbird.uitest.ui.AbstractDivView;
import com.vaadin.server.VaadinServlet;

public class SpringExample implements RouterConfigurator {
    // Used to verify that all instances are created through Spring
    private static final ThreadLocal<Boolean> inCreateBean = new ThreadLocal<>();

    /**
     * Represents Spring APIs used in the original example
     */
    private static class Spring {
        private static final String ROLE_ADMIN = "admin";
        private static final String ROLE_NOBODY = "nobody";

        // applicationContext.getBeanNamesForAnnotation(Route.class)
        public static List<String> getBeanNamesWithRoutes() {
            return Arrays.asList("view1", "view2", "view3");
        }

        // applicationContext.findAnnotationOnBean(view, Routes.class).value()
        public static String findRouteValueOnBean(String beanName) {
            if ("view1".equals(beanName)) {
                return "";
            }
            return beanName;
        }

        // applicationContext.findAnnotationOnBean(view, Secured.class).value()
        public static String[] findRolesForBean(String beanName) {
            switch (beanName) {
            case "view1":
                return new String[] { ROLE_NOBODY };
            case "view2":
                return new String[] { ROLE_ADMIN };
            default:
                return null;
            }
        }

        // accessDecisionVoter.vote(something, this, roles) == ACCESS_GRANTED
        public static boolean isAccessAllowed(String[] requiredRoles) {
            return requiredRoles == null
                    || Arrays.asList(requiredRoles).contains(ROLE_NOBODY);
        }

        // applicationContext.getType
        public static Class<?> getType(String beanName) {
            switch (beanName) {
            case "view1":
                return View1.class;
            case "view2":
                return View2.class;
            case "view3":
                return View3.class;
            default:
                throw new IllegalStateException("No such bean: " + beanName);
            }
        }

        // applicationContext.getBean
        public static Object getBean(String beanName) {
            try {
                inCreateBean.set(Boolean.TRUE);
                return getType(beanName).newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            } finally {
                inCreateBean.remove();
            }
        }

        // applicationContext.findAnnotationOnBean(view, ParentView.class)
        public static Class<? extends HasChildView> findParentViewForBean(
                String beanName) {
            switch (beanName) {
            case "view1":
                return MiddleView.class;
            case "view2":
                return RootView.class;
            default:
                return null;
            }
        }

        // Not really spring, but I'm too lazy to create an annotation for this
        // parentView.getAnnotation(ParentView.class)
        public static Class<? extends HasChildView> findParentViewForView(
                Class<? extends View> parentView) {
            if (parentView == MiddleView.class) {
                return RootView.class;
            } else {
                return null;
            }
        }
    }

    private static class AbstractSpringView extends AbstractDivView
            implements HasChildView {
        public AbstractSpringView() {
            if (inCreateBean.get() == null) {
                throw new IllegalStateException("Must be created by Spring");
            }
            setText(getClass().getSimpleName());
        }

        @Override
        public void setChildView(View childView) {
            getElement().setChild(1, Optional.of(childView)
                    .map(View::getElement).orElse(Element.createText("Empty")));
        }
    }

    public static class MiddleView extends AbstractSpringView {
    }

    public static class RootView extends AbstractSpringView {
    }

    public static class View1 extends AbstractSpringView {
    }

    public static class View2 extends AbstractSpringView {
    }

    public static class View3 extends AbstractSpringView {
    }

    // This is not an AbstractSpringView just to reduce some boilerplate in his
    // example
    public static class NoPermissionView extends AbstractDivView {
        public NoPermissionView() {
            setText("No permission");
        }
    }

    // Here starts the actual spring integration

    @WebServlet("/spring/*")
    @VaadinServletConfiguration(routerConfigurator = SpringExample.class, productionMode = false)
    public static class SpringExampleServlet extends VaadinServlet {
    }

    @Override
    public void configure(RouterConfiguration configuration) {
        Spring.getBeanNamesWithRoutes().forEach(
                beanName -> createRouteForBean(beanName, configuration));
    }

    private void createRouteForBean(String beanName,
            RouterConfiguration configuration) {
        String route = Spring.findRouteValueOnBean(beanName);

        Class<? extends View> beanType = Spring.getType(beanName)
                .asSubclass(View.class);

        // This is slightly ugly since the first parent is fetched by bean name
        // but others are fetched straight from the class
        List<Class<? extends HasChildView>> parentViews = getParentViews(
                Spring.findParentViewForBean(beanName));

        configuration.setRoute(route, new ViewRenderer() {
            @Override
            public Class<? extends View> getViewType() {
                String[] roles = Spring.findRolesForBean(beanName);
                if (roles != null && !Spring.isAccessAllowed(roles)) {
                    return NoPermissionView.class;
                } else {
                    return beanType;
                }
            }

            @Override
            public List<Class<? extends HasChildView>> getParentViewTypes(
                    Class<? extends View> viewType) {
                // Return value of getViewType() should really be passed to this
                // method so it wouldn't have to call it again
                if (viewType == NoPermissionView.class) {
                    return Collections.singletonList(RootView.class);
                } else {
                    return parentViews;
                }
            }

            @Override
            protected <T extends View> T getView(Class<T> viewType,
                    NavigationEvent event) {
                if (viewType == NoPermissionView.class) {
                    return (T) new NoPermissionView();
                } else {
                    return (T) Spring.getBean(beanName);
                }
            }
        });

    }

    private static List<Class<? extends HasChildView>> getParentViews(
            Class<? extends HasChildView> parentView) {
        List<Class<? extends HasChildView>> parentViews = new ArrayList<>();
        while (parentView != null) {
            parentViews.add(parentView);
            parentView = Spring.findParentViewForView(parentView);
        }
        return parentViews;
    }
}
