package com.vaadin.flow.spring.flowsecurity.service;

import javax.annotation.security.RolesAllowed;
import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.vaadin.flow.spring.flowsecurity.SecurityUtils;
import com.vaadin.flow.spring.flowsecurity.data.Account;

@Service
public class BankService {

    @Autowired
    private AccountService accountService;

    @Autowired
    private SecurityUtils utils;

    @RolesAllowed("user") // jsr250Enabled
    public void applyForLoan() {
        applyForLoan(10000);
    }

    @Secured("ROLE_admin") // securedEnabled
    public void applyForHugeLoan() {
        applyForLoan(1000000);
    }

    private void applyForLoan(int amount) {
        String name = utils.getAuthenticatedUser().getUsername();
        Optional<Account> acc = accountService.findByOwner(name);
        if (!acc.isPresent()) {
            return;
        }
        Account account = acc.get();
        account.setBalance(account.getBalance().add(new BigDecimal(amount)));
        accountService.save(account);
    }

    public BigDecimal getBalance() {
        String name = utils.getAuthenticatedUser().getUsername();
        return accountService.findByOwner(name).map(Account::getBalance)
                .orElse(null);
    }

}
