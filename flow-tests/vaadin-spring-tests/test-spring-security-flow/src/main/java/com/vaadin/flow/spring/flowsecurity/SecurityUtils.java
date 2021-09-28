package com.vaadin.flow.spring.flowsecurity;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.spring.flowsecurity.data.UserInfo;
import com.vaadin.flow.spring.flowsecurity.data.UserInfoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    @Autowired
    private UserInfoRepository userInfoRepository;
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
            UserDetails userDetails = (UserDetails) context.getAuthentication()
                    .getPrincipal();
            return userDetails;
        }
        // Anonymous or no authentication.
        return null;
    }

    public UserInfo getAuthenticatedUserInfo() {
        UserDetails details = getAuthenticatedUser();
        if (details == null) {
            return null;
        }
        return userInfoRepository.findByUsername(details.getUsername());
    }

    public void logout() {
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.setInvalidateHttpSession(false);
        logoutHandler.logout(
                VaadinServletRequest.getCurrent().getHttpServletRequest(), null,
                null);
        UI.getCurrent().getPage()
                .setLocation(securityConfig.getLogoutSuccessUrl());
    }

}
