package com.vaadin.flow.spring.flowsecurity.service;

import com.vaadin.flow.spring.flowsecurity.data.Account;
import com.vaadin.flow.spring.flowsecurity.data.UserInfo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AccountService {

    private static final Map<String, Account> ACCOUNT_CACHE = new HashMap<>(2);

    public AccountService(UserInfoService userInfoService) {
        UserInfo user = userInfoService.findByUsername("john");
        Account userAccount = new Account();
        userAccount.setOwner(user);
        userAccount.setBalance(new BigDecimal("10000"));

        UserInfo admin = userInfoService.findByUsername("emma");
        Account adminAccount = new Account();
        adminAccount.setOwner(admin);
        adminAccount.setBalance(new BigDecimal("200000"));

        ACCOUNT_CACHE.put(user.getUsername(), userAccount);
        ACCOUNT_CACHE.put(admin.getUsername(), adminAccount);
    }

    public Optional<Account> findByOwner(String username) {
        return Optional.ofNullable(ACCOUNT_CACHE.get(username));
    }

    public void save(Account account) {
        ACCOUNT_CACHE.put(account.getOwner().getUsername(), account);
    }
}
