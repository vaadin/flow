package com.vaadin.flow.server.dau;

import com.vaadin.pro.licensechecker.dau.EnforcementException;

/**
 * A DauEnforcementException is thrown when License Server imposes enforcement for the application and the
 * EnforcementRule check is not satisfied.
 * <p>
 * </p>
 * Wraps License Checker exception to simplify integration with Hilla and add-ons.
 * <p>
 * </p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 24.5
 */
public class DauEnforcementException extends RuntimeException {

    public DauEnforcementException(EnforcementException cause) {
        super(cause);
    }
}
