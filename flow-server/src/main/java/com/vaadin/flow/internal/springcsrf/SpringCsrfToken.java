/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.springcsrf;

import java.io.Serializable;

/**
 * A pojo for Spring CSRF token.
 */
public class SpringCsrfToken implements Serializable {
    private String headerName;
    private String parameterName;
    private String token;

    public SpringCsrfToken(String headerName, String parameterName,
            String token) {
        this.headerName = headerName;
        this.parameterName = parameterName;
        this.token = token;
    }

    public String getHeaderName() {
        return headerName;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getToken() {
        return token;
    }

}
