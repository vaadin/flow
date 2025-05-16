package com.vaadin.flow.spring.security;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.StandardClaimAccessor;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;

/**
 * Implementation of {@link VaadinUser} that assumes the authenticated principal implements
 * {@link StandardClaimAccessor}.
 */
final class OidcVaadinUser implements VaadinUser {

    private final String userId;
    private final String email;
    private final String fullName;
    private final String pictureUrl;
    private final String profileUrl;
    private final ZoneId timeZone;
    private final Locale locale;

    /**
     * Creates a new {@code OidcVaadinUser}.
     *
     * @param authentication the authentication object to use for extracting the user information.
     * @throws ClassCastException if the authentication principal is not an instance of {@link StandardClaimAccessor}.
     */
    OidcVaadinUser(Authentication authentication) {
        var claimAccessor = (StandardClaimAccessor) authentication.getPrincipal();
        var subject = claimAccessor.getSubject();

        userId = subject != null ? subject : authentication.getName();
        email = claimAccessor.getEmail();
        fullName = claimAccessor.getFullName();
        pictureUrl = claimAccessor.getPicture();
        profileUrl = claimAccessor.getProfile();
        timeZone = parseZoneId(claimAccessor.getZoneInfo());
        locale = parseLocale(claimAccessor.getLocale());
    }

    private static @Nullable ZoneId parseZoneId(@Nullable String zoneInfo) {
        try {
            return zoneInfo == null ? null : ZoneId.of(zoneInfo);
        } catch (Exception e) {
            return null;
        }
    }

    private static @Nullable Locale parseLocale(@Nullable String locale) {
        return locale == null ? null : Locale.forLanguageTag(locale);
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }

    @Override
    public Optional<String> getFullName() {
        return Optional.ofNullable(fullName);
    }

    @Override
    public Optional<String> getPictureUrl() {
        return Optional.ofNullable(pictureUrl);
    }

    @Override
    public Optional<String> getProfileUrl() {
        return Optional.ofNullable(profileUrl);
    }

    @Override
    public Optional<ZoneId> getTimeZone() {
        return Optional.ofNullable(timeZone);
    }

    @Override
    public Optional<Locale> getLocale() {
        return Optional.ofNullable(locale);
    }
}
