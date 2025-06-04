package com.vaadin.flow.spring.flowsecurity.views;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.spring.flowsecurity.SecurityUtils;
import com.vaadin.flow.spring.flowsecurity.data.UserInfo;

import static com.vaadin.flow.spring.flowsecurity.service.UserInfoService.ROLE_ADMIN;

public class MainView extends AppLayout {

    private final Tabs menu;
    private H1 viewTitle;
    private SecurityUtils securityUtils;
    private UserInfo userInfo;
    private AccessAnnotationChecker accessChecker;

    public MainView(SecurityUtils securityUtils,
            AccessAnnotationChecker accessChecker) {
        setId("main-view");
        this.securityUtils = securityUtils;
        this.accessChecker = accessChecker;
        userInfo = securityUtils.getAuthenticatedUserInfo();

        setPrimarySection(Section.DRAWER);
        addToNavbar(true, createHeaderContent());
        menu = createMenu();
        addToDrawer(createDrawerContent(menu));
    }

    private Component createHeaderContent() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.addClassName("header");
        layout.getThemeList().set("dark", true);
        layout.setWidthFull();
        layout.setSpacing(false);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.add(new DrawerToggle());
        viewTitle = new H1();
        layout.add(viewTitle);
        Avatar avatar = new Avatar();
        if (userInfo != null) {
            avatar.setName(userInfo.getFullName());
            avatar.setImage(userInfo.getImageUrl());
        }
        layout.add(avatar);
        return layout;
    }

    private Component createDrawerContent(Tabs menu) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.getThemeList().set("spacing-s", true);
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);
        HorizontalLayout logoLayout = new HorizontalLayout();
        logoLayout.addClassName("logo");
        logoLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        logoLayout
                .add(new Image("public/images/logo.jpg", "Bank of Flow logo"));
        logoLayout.add(new H1("Bank of Flow"));
        Div info = new Div();
        info.setText(
                "The menu intentionally shows items you cannot access so that access control can be tested by clicking on them");

        layout.add(logoLayout, info, menu);
        if (userInfo == null) {
            Button login = new Button("Log in");
            login.setId("login");
            login.addClickListener(e -> {
                e.getSource().getUI().get().navigate(LoginView.class);
            });
            layout.add(login);
        } else {
            if (securityUtils.getAuthenticatedUserInfo().getRoles()
                    .contains(ROLE_ADMIN)) {
                Button impersonate = new Button("Impersonate John",
                        e -> getUI().ifPresent(ui -> ui.getPage()
                                .setLocation("impersonate?username=john")));
                impersonate.setId("impersonate");
                layout.add(impersonate);
            } else if (SecurityContextHolder.getContext().getAuthentication()
                    .getAuthorities().stream().anyMatch(
                            auth -> SwitchUserFilter.ROLE_PREVIOUS_ADMINISTRATOR
                                    .equals(auth.getAuthority()))) {
                Button impersonate = new Button("Exit impersonation",
                        e -> getUI().ifPresent(ui -> ui.getPage()
                                .setLocation("impersonate/exit")));
                impersonate.setId("exit-impersonate");
                layout.add(impersonate);
            }
            Button logout = new Button("Logout");
            logout.setId("logout");
            logout.addClickListener(e -> {
                securityUtils.logout();
            });
            layout.add(logout);

            Button logoutFromServer = new Button("Logout from server");
            logoutFromServer.setId("logout-server");
            logoutFromServer.addClickListener(e -> {
                UI ui = UI.getCurrent();
                Runnable action = ui.accessLater(() -> securityUtils.logout(),
                        null);
                CompletableFuture.runAsync(action,
                        new DelegatingSecurityContextExecutor(CompletableFuture
                                .delayedExecutor(1, TimeUnit.SECONDS)));
            });
            layout.add(logoutFromServer);

            Anchor logoutWithUrl = new Anchor("doLogout", "Logout with URL");
            logoutWithUrl.getElement().setAttribute("router-ignore", true);
            logoutWithUrl.setId("logout-anchor");
            layout.add(logoutWithUrl);
        }
        return layout;
    }

    private Tabs createMenu() {
        final Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.addThemeVariants(TabsVariant.LUMO_MINIMAL);
        tabs.setId("tabs");
        tabs.add(createMenuItems());
        return tabs;
    }

    private Tab[] createMenuItems() {
        Tab[] tabs = new Tab[3];
        tabs[0] = createTab("Public", PublicView.class);
        if (accessChecker.hasAccess(PrivateView.class)) {
            tabs[1] = createTab("Private", PrivateView.class);
        } else {
            tabs[1] = createTab("Private (hidden)", PrivateView.class);
        }
        if (accessChecker.hasAccess(AdminView.class)) {
            tabs[2] = createTab("Admin", AdminView.class);
        } else {
            tabs[2] = createTab("Admin (hidden)", AdminView.class);
        }

        return tabs;
    }

    private static Tab createTab(String text,
            Class<? extends Component> navigationTarget) {
        final Tab tab = new Tab();
        tab.add(new RouterLink(text, navigationTarget));
        ComponentUtil.setData(tab, Class.class, navigationTarget);
        return tab;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        getTabForComponent(getContent()).ifPresent(menu::setSelectedTab);
        viewTitle.setText(getCurrentPageTitle());
    }

    private Optional<Tab> getTabForComponent(Component component) {
        return menu.getChildren()
                .filter(tab -> ComponentUtil.getData(tab, Class.class)
                        .equals(component.getClass()))
                .findFirst().map(Tab.class::cast);
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass()
                .getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }
}
