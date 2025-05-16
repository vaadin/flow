package com.vaadin.flow.spring.security;

import java.io.Serializable;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;

/**
 * Interface for accessing information about the current user. To provide a custom implementation, create a new
 * {@link org.springframework.security.core.userdetails.UserDetailsService} that return objects that implement both
 * the {@link org.springframework.security.core.userdetails.UserDetails} and this interface.
 */
public interface VaadinUser extends Serializable {

    /**
     * Returns the ID of the user. This can be a username, a UUID or some other identifier that uniquely identifies the
     * user. It can be used for things like audit logging, but should typically not be displayed to the user.
     *
     * @return the user ID (never {@code null}).
     */
    String getUserId();

    /**
     * Returns the email address of the user if available.
     *
     * @return the user's email address, or an empty {@code Optional} if not available.
     */
    default Optional<String> getEmail() {
        return Optional.empty();
    }

    /**
     * Returns the user's full name if available.
     *
     * @return the user's full name, or an empty {@code Optional} if not available.
     */
    default Optional<String> getFullName() {
        return Optional.empty();
    }

    /**
     * Returns the URL of the user's profile picture if available.
     *
     * @return the user's profile picture URL, or an empty {@code Optional} if not available.
     */
    default Optional<String> getPictureUrl() {
        return Optional.empty();
    }

    /**
     * Returns the URL of the user's profile page if available.
     *
     * @return the user's profile page URL, or an empty {@code Optional} if not available.
     */
    default Optional<String> getProfileUrl() {
        return Optional.empty();
    }

    /**
     * Returns the user's timezone if available.
     *
     * @return the user's timezone, or an empty {@code Optional} if not available.
     */
    default Optional<ZoneId> getTimeZone() {
        return Optional.empty();
    }

    /**
     * Returns the user's locale if available.
     *
     * @return the user's locale, or an empty {@code Optional} if not available.
     */
    default Optional<Locale> getLocale() {
        return Optional.empty();
    }
}
