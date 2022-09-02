package com.vaadin.flow.spring.flowsecurity.data;

import java.math.BigDecimal;

public class Account {

    private UserInfo owner;
    private BigDecimal balance;

    public UserInfo getOwner() {
        return owner;
    }

    public void setOwner(UserInfo owner) {
        this.owner = owner;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
