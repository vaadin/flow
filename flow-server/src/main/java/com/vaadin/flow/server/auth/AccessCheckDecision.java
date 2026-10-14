/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.auth;

/**
 * Decision on navigation access.
 */
public enum AccessCheckDecision {
    /**
     * Allows access to the target view.
     */
    ALLOW,
    /**
     * Denies access to the target view.
     */
    DENY,
    /**
     * Denies access to the target view because of a critical permission
     * configuration mistake.
     */
    REJECT,
    /**
     * Abstains from taking a decision about access to the target view.
     */
    NEUTRAL
}
