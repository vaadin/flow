/*
 * Copyright 2000-2022 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import com.vaadin.pro.licensechecker.Product;

public class ProductAndMessage implements DebugWindowData {
    private final Product product;
    private final String message;

    public ProductAndMessage(Product product, String message) {
        this.product = product;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public Product getProduct() {
        return product;
    }

    @Override
    public String toJson() {
        return String.format(
                "{\"product\": {\"name\": \"%s\", \"version\": \"%s\"}, \"message\": \"%s\"}",
                product.getName(), product.getVersion(), message);
    }
}