package com.vaadin.flow.spring.flowsecurity.service;

import com.vaadin.flow.spring.flowsecurity.data.UserInfo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserInfoService {

    public static String ROLE_USER = "user";
    public static String ROLE_ADMIN = "admin";

    private static final Map<String, UserInfo> USER_INFO_CACHE = new HashMap<>(
            2);

    public UserInfoService(PasswordEncoder encoder) {
        USER_INFO_CACHE.put("emma", new UserInfo("emma", encoder.encode("emma"),
                "Emma the Admin", "public/profiles/admin.svg", ROLE_ADMIN));
        USER_INFO_CACHE.put("john", new UserInfo("john", encoder.encode("john"),
                "John the User", "public/profiles/user.svg", ROLE_USER));
    }

    public UserInfo findByUsername(String username) {
        return USER_INFO_CACHE.get(username);
    }

}
