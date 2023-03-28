package com.vaadin.flow.spring.flowsecurity.service;

import com.vaadin.flow.spring.flowsecurity.data.Account;
import com.vaadin.flow.spring.flowsecurity.data.UserInfo;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AccountService {

    private static final ConcurrentMap<String, Account> ACCOUNT_CACHE = new ConcurrentHashMap<>(
            2);

    public AccountService(UserInfoService userInfoService) {
        UserInfo user = userInfoService.findByUsername("john");
        Account userAccount = new Account();
        userAccount.setOwner(user);
        userAccount.setBalance(new BigDecimal("10000.00"));

        UserInfo admin = userInfoService.findByUsername("emma");
        Account adminAccount = new Account();
        adminAccount.setOwner(admin);
        adminAccount.setBalance(new BigDecimal("200000.00"));

        ACCOUNT_CACHE.putIfAbsent(user.getUsername(), userAccount);
        ACCOUNT_CACHE.putIfAbsent(admin.getUsername(), adminAccount);
    }

    public Optional<Account> findByOwner(String username) {
        return Optional.ofNullable(ACCOUNT_CACHE.get(username));
    }

    public void save(Account account) {
        ACCOUNT_CACHE.put(account.getOwner().getUsername(), account);
    }
}
