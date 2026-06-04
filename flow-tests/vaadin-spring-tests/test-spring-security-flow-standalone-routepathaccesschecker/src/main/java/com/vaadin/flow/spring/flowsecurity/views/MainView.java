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
package com.vaadin.flow.spring.flowsecurity.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.flowsecurity.SecurityUtils;
import com.vaadin.flow.spring.flowsecurity.data.UserInfo;

@AnonymousAllowed
public class MainView extends Div
        implements RouterLayout, AfterNavigationObserver {

    private final Div tabs;
    private final Div content = new Div();
    private H1 viewTitle;
    private SecurityUtils securityUtils;
    private UserInfo userInfo;

    public MainView(SecurityUtils securityUtils) {
        setId("main-view");
        this.securityUtils = securityUtils;
        userInfo = securityUtils.getAuthenticatedUserInfo();

        getStyle().set("display", "flex").set("flex-direction", "column")
                .set("min-height", "100vh");

        Div navbar = createHeaderContent();
        tabs = createMenu();
        Div drawer = createDrawerContent(tabs);

        Div body = new Div(drawer, content);
        body.getStyle().set("display", "flex").set("flex", "1");
        content.getStyle().set("flex", "1");

        add(navbar, body);
    }

    private Div createHeaderContent() {
        Div layout = new Div();
        layout.addClassName("header");
        layout.getStyle().set("display", "flex").set("width", "100%")
                .set("align-items", "center");

        NativeButton drawerToggle = new NativeButton("☰");
        drawerToggle.setId("drawer-toggle");
        layout.add(drawerToggle);

        viewTitle = new H1();
        layout.add(viewTitle);

        Div avatar = new Div();
        avatar.addClassName("avatar");
        if (userInfo != null) {
            String image = userInfo.getImageUrl();
            if (image != null) {
                avatar.add(new Image(image, userInfo.getFullName()));
            } else {
                avatar.setText(userInfo.getFullName());
            }
        }
        layout.add(avatar);
        return layout;
    }

    private Div createDrawerContent(Div menu) {
        Div layout = new Div();
        layout.getStyle().set("display", "flex").set("flex-direction", "column")
                .set("padding", "0.5rem");

        Div logoLayout = new Div();
        logoLayout.addClassName("logo");
        logoLayout.getStyle().set("display", "flex").set("align-items",
                "center");
        logoLayout
                .add(new Image("public/images/logo.jpg", "Bank of Flow logo"));
        logoLayout.add(new H1("Bank of Flow"));
        Div info = new Div();
        info.setText(
                "The menu intentionally shows items you cannot access so that access control can be tested by clicking on them");

        layout.add(logoLayout, info, menu);
        if (userInfo == null) {
            NativeButton login = new NativeButton("Log in");
            login.setId("login");
            login.addClickListener(e -> {
                e.getSource().getUI().get().navigate(LoginView.class);
            });
            layout.add(login);
        } else {
            NativeButton logout = new NativeButton("Logout");
            logout.setId("logout");
            logout.addClickListener(e -> {
                securityUtils.logout();
            });
            layout.add(logout);
        }
        return layout;
    }

    private Div createMenu() {
        Div nav = new Div();
        nav.setId("tabs");
        nav.getStyle().set("display", "flex").set("flex-direction", "column");
        nav.add(menuItem("Public", PublicView.class));
        nav.add(menuItem("Private", PrivateView.class));
        nav.add(menuItem("Admin", AdminView.class));
        return nav;
    }

    private static RouterLink menuItem(String text,
            Class<? extends Component> navigationTarget) {
        RouterLink link = new RouterLink(text, navigationTarget);
        link.getElement().setAttribute("data-target",
                navigationTarget.getName());
        return link;
    }

    @Override
    public void showRouterLayoutContent(HasElement contentElement) {
        content.removeAll();
        if (contentElement != null) {
            content.getElement().appendChild(contentElement.getElement());
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        Component current = currentContent();
        if (current == null) {
            viewTitle.setText("");
            return;
        }
        String targetName = current.getClass().getName();
        tabs.getChildren().forEach(child -> {
            boolean selected = targetName
                    .equals(child.getElement().getAttribute("data-target"));
            child.getElement().setAttribute("aria-current",
                    selected ? "page" : "false");
        });
        viewTitle.setText(pageTitle(current));
    }

    private Component currentContent() {
        return content.getChildren().findFirst().orElse(null);
    }

    private static String pageTitle(Component current) {
        PageTitle title = current.getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }
}
