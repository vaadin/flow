package com.vaadin.flow.spring.flowsecurity.data;

import java.math.BigDecimal;
import java.util.UUID;

public class Account {

    private UUID id;
    private UserInfo owner;
    private BigDecimal balance;

    public UUID getId() {
        return id;
    }

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
