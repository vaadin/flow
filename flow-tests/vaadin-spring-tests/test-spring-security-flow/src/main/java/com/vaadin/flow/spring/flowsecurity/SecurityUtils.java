package com.vaadin.flow.spring.flowsecurity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.UrlUtil;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.spring.flowsecurity.data.UserInfo;
import com.vaadin.flow.spring.flowsecurity.service.UserInfoService;

@Component
public class SecurityUtils {

    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private SecurityConfig securityConfig;

    public UserDetails getAuthenticatedUser() {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context == null) {
            throw new IllegalStateException("No security context available");
        }
        if (context.getAuthentication() == null) {
            return null;
        }
        Object principal = context.getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return (UserDetails) context.getAuthentication().getPrincipal();
        }
        // Anonymous or no authentication.
        return null;
    }

    public UserInfo getAuthenticatedUserInfo() {
        UserDetails details = getAuthenticatedUser();
        if (details == null) {
            return null;
        }
        return userInfoService.findByUsername(details.getUsername());
    }

    public void logout() {
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.setInvalidateHttpSession(false);
        VaadinServletRequest request = VaadinServletRequest.getCurrent();
        logoutHandler.logout(request, null, null);
        UI.getCurrent().getPage().setLocation(UrlUtil.getServletPathRelative(
                securityConfig.getLogoutSuccessUrl(), request));
    }

}
