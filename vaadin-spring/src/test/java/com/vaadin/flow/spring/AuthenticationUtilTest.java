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
package com.vaadin.flow.spring;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationUtilTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getSecurityHolderRoleChecker_nullAuthority_ignored() {
        GrantedAuthority nullAuthority = () -> null;
        SecurityContextHolder.getContext()
                .setAuthentication(UsernamePasswordAuthenticationToken
                        .authenticated("user", null, List.of(nullAuthority,
                                new SimpleGrantedAuthority("ROLE_USER"))));

        var roleChecker = AuthenticationUtil.getSecurityHolderRoleChecker();
        assertTrue(roleChecker.apply("USER"));
        assertFalse(roleChecker.apply("ADMIN"));
    }
}
