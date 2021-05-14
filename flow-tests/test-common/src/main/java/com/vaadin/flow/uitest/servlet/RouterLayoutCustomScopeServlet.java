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

package com.vaadin.flow.uitest.servlet;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;

@WebServlet(asyncSupported = true, urlPatterns = {
        "/router-layout-custom-scope/*" })
public class RouterLayoutCustomScopeServlet extends VaadinServlet {

    public static String NAVIGATE_TO_ANOTHER_ROUTE_INSIDE_MAIN_LAYOUT_BUTTON_ID = "navigate-to-another-route-inside-main-layout-button-id";
    public static String NAVIGATE_TO_ANOTHER_ROUTE_OUTSIDE_MAIN_LAYOUT_BUTTON_ID = "navigate-to-another-layout-button-id";
    public static String NAVIGATE_BACK_FROM_ANOTHER_ROUTE_INSIDE_MAIN_LAYOUT_BUTTON_ID = "navigate-back-from-another-route-inside-main-layout-button-id";
    public static String NAVIGATE_BACK_FROM_ANOTHER_ROUTE_OUTSIDE_MAIN_LAYOUT_BUTTON_ID = "navigate-back-from-another-route-outside-main-layout-button-id";
    public static String SUB_LAYOUT_ID = "sub-layout-id";

    @Override
    protected VaadinServletService createServletService(
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        RouterLayoutCustomScopeService routerLayoutCustomScopeService = new RouterLayoutCustomScopeService(
                this, deploymentConfiguration);
        routerLayoutCustomScopeService.init();
        return routerLayoutCustomScopeService;
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CustomUIScope {
    }

    private static class CustomSessionScopeContext {
        private static final String uiScopeContextKey = CustomSessionScopeContext.class
                .getName();

        static CustomUIScopeContext getUIScopeContext() {
            VaadinSession current = VaadinSession.getCurrent();
            CustomUIScopeContext uiScopeContext = (CustomUIScopeContext) current
                    .getAttribute(uiScopeContextKey);
            if (uiScopeContext == null) {
                uiScopeContext = new CustomUIScopeContext();
                current.setAttribute(uiScopeContextKey, uiScopeContext);
            }
            return uiScopeContext;
        }
    }

    private static class CustomUIScopeContext {
        // Modifying UI and components are supposed to be within a single
        // test, so it's not necessary to use thread-safe collection or
        // synchronization
        private final Map<Integer, Map<Class<? extends Component>, Component>> components = new HashMap<>();

        void addUI(UI ui) {
            components.put(ui.getUIId(), new HashMap<>());
            // Cleanup the components context upon detaching UI
            ui.addDetachListener(event -> {
                Map<Class<? extends Component>, Component> removed = components
                        .remove(event.getUI().getUIId());
                removed.clear();
            });
        }

        Component getComponent(Class<? extends Component> componentType,
                SerializableFunction<Class<? extends Component>, Component> instanceSupplier) {
            UI current = UI.getCurrent();
            assert current != null : "Current UI is supposed to be not empty "
                    + "when a component instance is being requested";
            components.get(current.getUIId()).computeIfAbsent(componentType,
                    instanceSupplier);
            return components.get(current.getUIId()).get(componentType);
        }
    }

    private static class RouterLayoutCustomScopeService
            extends VaadinServletService {

        public RouterLayoutCustomScopeService(VaadinServlet servlet,
                DeploymentConfiguration deploymentConfiguration) {
            super(servlet, deploymentConfiguration);
            // Create UIScope context upon entering a new UI (browser tab/new
            // test)
            addUIInitListener(event -> CustomSessionScopeContext
                    .getUIScopeContext().addUI(event.getUI()));
        }

        @Override
        protected Instantiator createInstantiator() {
            RouterLayoutCustomScopeInstantiator routerLayoutCustomScopeInstantiator = new RouterLayoutCustomScopeInstantiator(
                    this);
            routerLayoutCustomScopeInstantiator.init(this);
            return routerLayoutCustomScopeInstantiator;
        }
    }

    private static class RouterLayoutCustomScopeInstantiator
            extends DefaultInstantiator {

        public RouterLayoutCustomScopeInstantiator(VaadinService service) {
            super(service);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getOrCreate(Class<T> type) {
            if (Component.class.isAssignableFrom(type)) {
                CustomUIScope annotation = type
                        .getAnnotation(CustomUIScope.class);
                if (annotation != null) {
                    return (T) CustomSessionScopeContext.getUIScopeContext()
                            .getComponent((Class<? extends Component>) type,
                                    super::getOrCreate);
                }
            }
            return super.getOrCreate(type);
        }
    }

    @CustomUIScope
    @Route("main")
    public static class CustomUIScopeMainLayout extends Div
            implements RouterLayout {

        public CustomUIScopeMainLayout() {
            add(new Span("This is a topmost parent router layout"));
        }

        @CustomUIScope
        @ParentLayout(CustomUIScopeMainLayout.class)
        public static class SubLayout extends Div implements RouterLayout {
            public SubLayout() {
                setId(SUB_LAYOUT_ID);
                add(new Span("This is a sub router layout"));
            }
        }

        @Route(value = "first-child-route", layout = SubLayout.class)
        public static class FirstView extends Div {

            public FirstView() {
                add(new Span("This is a child route inside main layout"));
                NativeButton navigateToAnotherViewButton = new NativeButton(
                        "Navigate to another route inside Main Layout",
                        click -> UI.getCurrent().navigate(SecondView.class));
                navigateToAnotherViewButton.setId(
                        NAVIGATE_TO_ANOTHER_ROUTE_INSIDE_MAIN_LAYOUT_BUTTON_ID);
                add(navigateToAnotherViewButton);
                NativeButton navigateToAnotherLayoutButton = new NativeButton(
                        "Navigate to another route outside Main Layout",
                        click -> UI.getCurrent().navigate(
                                CustomUIScopeAnotherLayout.ThirdView.class));
                navigateToAnotherLayoutButton.setId(
                        NAVIGATE_TO_ANOTHER_ROUTE_OUTSIDE_MAIN_LAYOUT_BUTTON_ID);
                add(navigateToAnotherLayoutButton);
            }
        }

        @Route(value = "second-child-route", layout = CustomUIScopeMainLayout.class)
        public static class SecondView extends Div {

            public SecondView() {
                add(new Span("This is another route inside Main Layout"));
                NativeButton navigateToChildView = new NativeButton(
                        "Navigate to first route",
                        click -> UI.getCurrent().navigate(FirstView.class));
                navigateToChildView.setId(
                        NAVIGATE_BACK_FROM_ANOTHER_ROUTE_OUTSIDE_MAIN_LAYOUT_BUTTON_ID);
                add(navigateToChildView);
            }
        }
    }

    @CustomUIScope
    @Route("secondary")
    public static class CustomUIScopeAnotherLayout extends Div
            implements RouterLayout {

        public CustomUIScopeAnotherLayout() {
            add(new Span("This is an another topmost parent router layout"));
        }

        @Route(value = "third-child-route", layout = CustomUIScopeAnotherLayout.class)
        public static class ThirdView extends Div {

            public ThirdView() {
                add(new Span("This is another route outside of Main Layout"));
                NativeButton navigateToChildView = new NativeButton(
                        "Navigate to first view",
                        click -> UI.getCurrent().navigate(
                                CustomUIScopeMainLayout.FirstView.class));
                navigateToChildView.setId(
                        NAVIGATE_BACK_FROM_ANOTHER_ROUTE_INSIDE_MAIN_LAYOUT_BUTTON_ID);
                add(navigateToChildView);
            }
        }
    }
}
