package com.vaadin.flow.spring.flowsecurity.service;

import java.math.BigDecimal;
import java.util.Optional;

import com.vaadin.flow.spring.flowsecurity.SecurityUtils;
import com.vaadin.flow.spring.flowsecurity.data.Account;
import com.vaadin.flow.spring.flowsecurity.data.AccountRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BankService {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private SecurityUtils utils;

    public void applyForLoan() {
        applyForLoan(10000);
    }

    public void applyForHugeLoan() {
        applyForLoan(1000000);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }
    }

    private void applyForLoan(int amount) {
        String name = utils.getAuthenticatedUser().getUsername();
        Optional<Account> acc = accountRepository.findByOwner(name);
        if (!acc.isPresent()) {
            return;
        }
        Account account = acc.get();
        account.setBalance(account.getBalance().add(new BigDecimal(amount)));
        accountRepository.save(account);
    }

    public BigDecimal getBalance() {
        String name = utils.getAuthenticatedUser().getUsername();
        return accountRepository.findByOwner(name).map(Account::getBalance)
                .orElse(null);
    }

}
