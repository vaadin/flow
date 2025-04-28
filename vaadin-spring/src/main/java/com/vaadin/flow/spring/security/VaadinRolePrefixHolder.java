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

package com.vaadin.flow.spring.security;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.util.FieldUtils;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;

/**
 * Holds role prefix accessible outside an active request. Role prefix should
 * match with {@link SecurityContextHolderAwareRequestWrapper} in Spring
 * Security context aware environments to allow utilities like
 * {@link com.vaadin.flow.spring.AuthenticationUtil} to check roles in the same
 * way with same role prefix.
 */
public class VaadinRolePrefixHolder implements Serializable {

    private String rolePrefix;

    private boolean rolePrefixSet;

    /**
     * Construct {@link VaadinRolePrefixHolder} with a role prefix.
     *
     * @param rolePrefix
     *            Role prefix.
     */
    public VaadinRolePrefixHolder(String rolePrefix) {
        this.rolePrefix = rolePrefix;
        this.rolePrefixSet = this.rolePrefix != null;
    }

    /**
     * Return role prefix. May be null.
     *
     * @return Role prefix
     */
    public String getRolePrefix() {
        return rolePrefix;
    }

    /**
     * Gets boolean flag indicating if role prefix is set or not.
     *
     * @return true when role prefix is set, false otherwise.
     */
    public boolean isSet() {
        return rolePrefixSet;
    }

    /**
     * Reset role prefix from the given request. Works only with a
     * {@link VaadinServletRequest} and a chain of
     * {@link ServletRequestWrapper}s to find
     * {@link SecurityContextHolderAwareRequestWrapper} with the role prefix.
     * Method doesn't do anything if role prefix is not found.
     *
     * @param request
     *            Vaadin request used to find active role prefix.
     */
    public void resetRolePrefix(VaadinRequest request) {
        SecurityContextHolderAwareRequestWrapper requestWrapper = findSecurityContextHolderAwareRequestWrapper(
                request);
        resetRolePrefix(requestWrapper);
    }

    /**
     * Reset role prefix from the given {@link DefaultSecurityFilterChain}.
     * Method doesn't do anything if role prefix is not found.
     *
     * @param defaultSecurityFilterChain
     *            Default security filter chain used to find active role prefix.
     * @throws NullPointerException
     *             If <code>defaultSecurityFilterChain</code> is null.
     */
    public void resetRolePrefix(
            DefaultSecurityFilterChain defaultSecurityFilterChain) {
        defaultSecurityFilterChain.getFilters().stream()
                .filter(filter -> SecurityContextHolderAwareRequestFilter.class
                        .isAssignableFrom(filter.getClass()))
                .map(SecurityContextHolderAwareRequestFilter.class::cast)
                .findFirst().ifPresent(this::resetRolePrefix);
    }

    void resetRolePrefix(
            SecurityContextHolderAwareRequestFilter securityContextHolderAwareRequestFilter) {
        resetRolePrefix(securityContextHolderAwareRequestFilter,
                SecurityContextHolderAwareRequestFilter.class);
    }

    void resetRolePrefix(
            SecurityContextHolderAwareRequestWrapper securityContextHolderAwareRequestWrapper) {
        resetRolePrefix(securityContextHolderAwareRequestWrapper,
                SecurityContextHolderAwareRequestWrapper.class);
    }

    void resetRolePrefix(Object source, Class<?> type) {
        if (source != null) {
            try {
                Field field = FieldUtils.getField(type, "rolePrefix");
                field.setAccessible(true);
                rolePrefix = (String) field.get(source);
            } catch (IllegalAccessException e) {
                getLogger().warn(
                        String.format("Could not read %s#rolePrefix field.",
                                type.getSimpleName()),
                        e);
            }
        }
        this.rolePrefixSet = true;
    }

    private SecurityContextHolderAwareRequestWrapper findSecurityContextHolderAwareRequestWrapper(
            VaadinRequest request) {
        if (request instanceof VaadinServletRequest) {
            ServletRequest servletRequest = ((VaadinServletRequest) request)
                    .getRequest();
            Set<Object> checkedWrappers = new HashSet<>();
            while (servletRequest instanceof ServletRequestWrapper
                    && !checkedWrappers.contains(servletRequest)) {
                checkedWrappers.add(servletRequest);
                if (SecurityContextHolderAwareRequestWrapper.class
                        .isAssignableFrom(servletRequest.getClass())) {
                    return (SecurityContextHolderAwareRequestWrapper) servletRequest;
                }
                servletRequest = ((ServletRequestWrapper) servletRequest)
                        .getRequest();
            }
        }
        return null;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(VaadinRolePrefixHolder.class);
    }
}
