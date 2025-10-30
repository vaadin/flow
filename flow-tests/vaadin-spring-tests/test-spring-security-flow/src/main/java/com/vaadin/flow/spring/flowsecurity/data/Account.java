/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
