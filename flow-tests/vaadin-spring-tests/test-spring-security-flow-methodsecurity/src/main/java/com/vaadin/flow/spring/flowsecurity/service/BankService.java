package com.vaadin.flow.spring.flowsecurity.service;

import java.math.BigDecimal;
import java.util.Optional;

import com.vaadin.flow.spring.flowsecurity.data.Account;
import com.vaadin.flow.spring.security.AuthenticationContext;

import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class BankService {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AuthenticationContext authenticationContext;

    @RolesAllowed("user") // jsr250Enabled
    public void applyForLoan() {
        applyForLoan(10000);
    }

    @Secured("ROLE_admin") // securedEnabled
    public void applyForHugeLoan() {
        applyForLoan(1000000);
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
