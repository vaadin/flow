package com.vaadin.flow.spring.hillasecurity.endpoints;

import javax.annotation.Nullable;

import dev.hilla.Endpoint;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.hillasecurity.SecurityUtils;
import com.vaadin.flow.spring.hillasecurity.data.UserInfo;

import org.springframework.beans.factory.annotation.Autowired;

@Endpoint
@AnonymousAllowed
public class UserInfoEndpoint {

    @Autowired
    private SecurityUtils utils;

    public UserInfo getUserInfo() {
        return utils.getAuthenticatedUserInfo();
    }
}
