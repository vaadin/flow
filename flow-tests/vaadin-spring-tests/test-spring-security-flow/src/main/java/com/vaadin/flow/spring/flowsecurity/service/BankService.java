package com.vaadin.flow.spring.flowsecurity.service;

import java.math.BigDecimal;
import java.util.Optional;

import com.vaadin.flow.spring.flowsecurity.data.Account;
import com.vaadin.flow.spring.security.AuthenticationContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class BankService {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AuthenticationContext authenticationContext;

    public void applyForLoan() {
        applyForLoan(10000);
    }

    public void applyForHugeLoan() {
        applyForLoan(1000000);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignored) {
        }
    }

    private void applyForLoan(int amount) {
        Optional<UserDetails> authenticatedUser = authenticationContext
                .getAuthenticatedUser(UserDetails.class);
        if (authenticatedUser.isEmpty()) {
            return;
        }
        Optional<Account> acc = accountService
                .findByOwner(authenticatedUser.get().getUsername());
        if (acc.isEmpty()) {
            return;
        }
        Account account = acc.get();
        account.setBalance(account.getBalance().add(new BigDecimal(amount)));
        accountService.save(account);
    }

    public BigDecimal getBalance() {
        Optional<UserDetails> authenticatedUser = authenticationContext
                .getAuthenticatedUser(UserDetails.class);
        if (authenticatedUser.isEmpty()) {
            return null;
        }
        return accountService.findByOwner(authenticatedUser.get().getUsername())
                .map(Account::getBalance).orElse(null);
    }

}
