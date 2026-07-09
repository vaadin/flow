/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.base.devserver;

import java.io.Serializable;

import com.vaadin.pro.licensechecker.PreTrial;
import com.vaadin.pro.licensechecker.Product;

class ProductAndMessage implements Serializable {
    private final Product product;
    private final String message;
    private final PreTrial preTrial;

    public ProductAndMessage(Product product, String message) {
        this.product = product;
        this.message = message;
        this.preTrial = null;
    }

    public ProductAndMessage(Product product, PreTrial preTrial,
            String message) {
        this.product = product;
        this.preTrial = preTrial;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public Product getProduct() {
        return product;
    }

    public PreTrial getPreTrial() {
        return preTrial;
    }
}
