package com.vaadin.flow.spring.flowsecurity.views;

import jakarta.annotation.security.RolesAllowed;

import java.util.concurrent.TimeUnit;

import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.flowsecurity.SecurityUtils;

@Route(value = "admin", layout = MainView.class)
@PageTitle("Admin View")
@RolesAllowed("admin")
@Menu(order = 3)
public class AdminView extends VerticalLayout {

    public final static String ROLE_PREFIX_TEST_BUTTON_ID = "role-prefix-test-button";

    public AdminView(SecurityUtils securityUtils) {
        H1 welcome = new H1("Welcome to the admin page, "
                + securityUtils.getAuthenticatedUserInfo().getFullName());
        welcome.setId("welcome");
        add(welcome);
        Div div = new Div();
        div.setText(
                "This page is full of dangerous controls and secret information");
        add(div);

        Button accessRolePrefixedAdminPageFromThread = new Button(
                "Access ROLE_ prefixed admin view from another thread");
        accessRolePrefixedAdminPageFromThread.setId(ROLE_PREFIX_TEST_BUTTON_ID);
        accessRolePrefixedAdminPageFromThread.addClickListener(event -> {
            UI ui = event.getSource().getUI().get();
            Runnable doNavigation = new DelegatingSecurityContextRunnable(
                    () -> ui.navigate(RolePrefixedAdminView.class),
                    SecurityContextHolder.getContext());
            Runnable delayedNavigation = () -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                    ui.access(doNavigation::run);
                } catch (InterruptedException e) {
                }
            };
            new Thread(delayedNavigation).start();
        });
        add(accessRolePrefixedAdminPageFromThread);
    }
}
