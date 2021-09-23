package com.vaadin.flow.internal.springcsrf;

import java.io.Serializable;

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
