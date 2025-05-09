package com.vaadin.flow.spring.flowsecurity;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.vaadin.flow.spring.flowsecurity.data.UserInfo;
import com.vaadin.flow.spring.flowsecurity.service.UserInfoService;
import com.vaadin.flow.spring.security.AuthenticationContext;

@Component
public class SecurityUtils {

    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private AuthenticationContext authenticationContext;

    public UserInfo getAuthenticatedUserInfo() {
        Optional<UserDetails> userDetails = authenticationContext
                .getAuthenticatedUser(UserDetails.class);
        if (userDetails.isEmpty()) {
            return null;
        }
        return userInfoService.findByUsername(userDetails.get().getUsername());
    }

    public void logout() {
        authenticationContext.logout();
    }

}
