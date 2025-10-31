/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.spring.flowsecurity;

import java.util.Collection;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
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

    public SecurityContext getSecurityContext() {
        return authenticationContext.getSecurityContext();
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authenticationContext.getGrantedAuthorities();
    }

    public void logout() {
        authenticationContext.logout();
    }

}
