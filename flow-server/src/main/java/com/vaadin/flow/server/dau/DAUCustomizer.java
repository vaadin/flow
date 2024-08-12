package com.vaadin.flow.server.dau;

import java.io.Serializable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.SystemMessagesInfo;
import com.vaadin.flow.server.VaadinSession;

/**
 * Interface to be implemented to customize Daily Active Users feature.
 * <p>
 * </p>
 * By implementing this interface it is possible to:
 * <ul>
 * <li>provide an user identity supplier to allow the system to count a user
 * once even if it accesses the application on multiple devices</li>
 * <li>provide custom messages and a landing page for the enforcement
 * notification popup</li>
 * </ul>
 *
 * Only one implementation is allowed and it is discovered through the Vaadin
 * {@link com.vaadin.flow.di.Instantiator}.
 *
 * @see com.vaadin.flow.di.Instantiator
 * @see EnforcementNotificationMessages
 * @see UserIdentitySupplier
 * @since 24.5
 */
public interface DAUCustomizer extends Serializable {

    /**
     * Gets the enforcement messages to use in the given context. The
     * {@link SystemMessagesInfo} object contains available information but in
     * most cases some or both of {@link VaadinSession#getCurrent()} and
     * {@link UI#getCurrent()} can also be used to find more information to help
     * the decision.
     * <p>
     * </p>
     * The default implementation returns
     * {@link EnforcementNotificationMessages#DEFAULT}.
     *
     * @param systemMessagesInfo
     *            Locale, current request and other information available.
     * @return an enforcement messages object, never {@literal null}.
     */
    default EnforcementNotificationMessages getEnforcementNotificationMessages(
            SystemMessagesInfo systemMessagesInfo) {
        return EnforcementNotificationMessages.DEFAULT;
    }

    /**
     * Gets the function to be used to determine the user identity for the
     * current request.
     * <p>
     * </p>
     * By default, returns {@literal null}, meaning that user identity is not
     * computed.
     *
     * @return the function to be used to determine the user identity for the
     *         current request. Can be {@literal null}.
     */
    default UserIdentitySupplier getUserIdentitySupplier() {
        return null;
    }

}
