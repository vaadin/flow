/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.enumtype;

import java.util.List;

import com.vaadin.fusion.Endpoint;

@Endpoint
public class EnumEndpoint {
    public enum MyEnum {
        ENUM1(1), ENUM2(2), ENUM_2(2), HELLO_WORLD(3), _HELLO(
                4), MANY_MANY_WORDS(5);

        private final int value;

        MyEnum(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private MyEnum value = MyEnum.ENUM1;

    public MyEnum getEnum() {
        return value;
    }

    public void setEnum(MyEnum value) {
        this.value = value;
    }

    public MyEnum echoEnum(MyEnum value) {
        return value;
    }

    public List<MyEnum> echoListEnum(List<MyEnum> enumList) {
        return enumList;
    }
}
