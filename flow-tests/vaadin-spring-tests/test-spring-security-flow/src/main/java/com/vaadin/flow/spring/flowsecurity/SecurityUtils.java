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
import com.vaadin.flow.spring.security.AuthenticationContext;

@Component
public class SecurityUtils {

    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private SecurityConfig securityConfig;
    @Autowired
    private AuthenticationContext authenticationContext;

    public UserInfo getAuthenticatedUserInfo() {
        UserDetails details = authenticationContext.getAuthenticatedUser().orElseThrow();
        if (details == null) {
            return null;
        }
        return userInfoService.findByUsername(details.getUsername());
    }

    public void logout() {
        authenticationContext.logout();
        UI.getCurrent().getPage().setLocation(UrlUtil.getServletPathRelative(
                securityConfig.getLogoutSuccessUrl(),
                VaadinServletRequest.getCurrent().getHttpServletRequest()));
    }

}
