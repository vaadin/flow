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
package com.vaadin.flow.spring.flowsecurity.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.vaadin.flow.spring.flowsecurity.data.UserInfo;

@Service
public class UserInfoService {

    public static String ROLE_USER = "user";
    public static String ROLE_ADMIN = "admin";

    private static final ConcurrentMap<String, UserInfo> USER_INFO_CACHE = new ConcurrentHashMap<>(
            2);

    public UserInfoService(PasswordEncoder encoder) {
        USER_INFO_CACHE.putIfAbsent("emma",
                new UserInfo("emma", encoder.encode("emma"), "Emma the Admin",
                        "public/profiles/admin.svg", ROLE_ADMIN));
        USER_INFO_CACHE.putIfAbsent("john",
                new UserInfo("john", encoder.encode("john"), "John the User",
                        "public/profiles/user.svg", ROLE_USER));
    }

    public UserInfo findByUsername(String username) {
        return USER_INFO_CACHE.get(username);
    }

}
