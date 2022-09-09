package com.vaadin.flow.spring.flowsecurity.data;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class Account {

    private UserInfo owner;
    private final AtomicReference<BigDecimal> balance = new AtomicReference<>(
            BigDecimal.ZERO);

    public UserInfo getOwner() {
        return owner;
    }

    public void setOwner(UserInfo owner) {
        this.owner = owner;
    }

    public BigDecimal getBalance() {
        return balance.get();
    }

    public void setBalance(BigDecimal newBalance) {
        this.balance.compareAndSet(getBalance(), newBalance);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Account account = (Account) o;
        return owner.equals(account.owner)
                && getBalance().equals(account.getBalance());
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, balance);
    }
}
