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
package com.vaadin.flow.tests.data.bean;

import java.io.Serializable;

public class FatherAndSon implements Serializable {
    private String firstName;
    private String lastName;
    private FatherAndSon father;
    private FatherAndSon son;

    public FatherAndSon() {

    }

    @Override
    public String toString() {
        return "FatherAndSon [firstName=" + firstName + ", lastName=" + lastName
                + ", father=" + father + ", son=" + son + "]";
    }

    public FatherAndSon(String firstName, String lastName, FatherAndSon father,
            FatherAndSon son) {
        super();
        this.firstName = firstName;
        this.lastName = lastName;
        this.father = father;
        if (this.father != null)
            this.father.setSon(this);
        else
            this.son = son;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public FatherAndSon getFather() {
        return father;
    }

    public void setFather(FatherAndSon father) {
        this.father = father;
    }

    public FatherAndSon getSon() {
        return son;
    }

    public void setSon(FatherAndSon son) {
        this.son = son;
    }

}
