/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserSource;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ProviderDetails;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ProviderDetails.UserInfoEndpoint;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Specification of the opt-in mapping of Keycloak realm and client roles to
 * Spring Security granted authorities.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KeycloakOidcUserMapperTest {

    private static final String CLIENT_ID = "test-client";

    private static final String ISSUER_URI = "http://localhost:8080/realms/test";

    @Mock
    private OidcUserSource userSource;

    @Mock
    private OidcUserRequest userRequest;

    @Mock
    private OidcUserInfo userInfo;

    @Mock
    private OidcIdToken idToken;

    @Mock
    private ClientRegistration clientRegistration;

    @Mock
    private ProviderDetails providerDetails;

    @Mock
    private UserInfoEndpoint userInfoEndpoint;

    @Mock
    private OAuth2AccessToken accessToken;

    private final Map<String, Object> accessTokenClaims = new HashMap<>();

    private final AtomicInteger decoderFactoryCalls = new AtomicInteger();

    private KeycloakOidcUserMapper mapper;

    @BeforeEach
    void setup() {
        mapper = new KeycloakOidcUserMapper(null, registration -> {
            decoderFactoryCalls.incrementAndGet();
            return this::decode;
        });

        when(userSource.getUserRequest()).thenReturn(userRequest);
        when(userSource.getUserInfo()).thenReturn(userInfo);
        when(userRequest.getClientRegistration())
                .thenReturn(clientRegistration);
        when(userRequest.getAccessToken()).thenReturn(accessToken);
        when(userRequest.getIdToken()).thenReturn(idToken);
        when(clientRegistration.getRegistrationId()).thenReturn("keycloak");
        when(clientRegistration.getClientId()).thenReturn(CLIENT_ID);
        when(clientRegistration.getProviderDetails())
                .thenReturn(providerDetails);
        when(providerDetails.getUserInfoEndpoint())
                .thenReturn(userInfoEndpoint);
        when(accessToken.getTokenValue()).thenReturn("token");
        when(accessToken.getScopes()).thenReturn(Set.of("openid", "profile"));
        when(idToken.getClaims())
                .thenReturn(Map.of("sub", "user-123", "iss", ISSUER_URI));
    }

    @Test
    void convert_realmAndClientRolesAndScopesMapped() {
        accessTokenClaims.put("realm_access",
                Map.of("roles", List.of("admin", "user")));
        accessTokenClaims.put("resource_access",
                Map.of(CLIENT_ID, Map.of("roles", List.of("manage-account"))));

        var authorities = authorities();

        assertThat(authorities).contains("ROLE_admin", "ROLE_user",
                "ROLE_manage-account", "SCOPE_openid", "SCOPE_profile");
        assertThat(mapper.convert(userSource).getAuthorities())
                .hasAtLeastOneElementOfType(OidcUserAuthority.class);
    }

    @Test
    void convert_otherClientRolesAndMissingClaimsIgnored() {
        accessTokenClaims.put("resource_access",
                Map.of("other-client", Map.of("roles", List.of("other-role"))));

        assertThat(authorities()).noneMatch(a -> a.startsWith("ROLE_"));
    }

    @Test
    void convert_customRolePrefixAppliedToRolesOnly() {
        mapper = new KeycloakOidcUserMapper(() -> "AUTHORITY_",
                registration -> this::decode);
        accessTokenClaims.put("realm_access",
                Map.of("roles", List.of("admin")));

        assertThat(authorities()).contains("AUTHORITY_admin", "SCOPE_openid")
                .doesNotContain("ROLE_admin");
    }

    @Test
    void convert_accessTokenNotAJwt_mappedWithoutRoles() {
        mapper = new KeycloakOidcUserMapper(null, registration -> token -> {
            throw new BadJwtException("not a JWT");
        });

        var authorities = authorities();

        assertThat(authorities).noneMatch(a -> a.startsWith("ROLE_"));
        assertThat(authorities).contains("SCOPE_openid");
    }

    @Test
    void convert_userNameAttributeUsedAsName_userInfoRetained() {
        when(userInfoEndpoint.getUserNameAttributeName())
                .thenReturn("preferred_username");
        when(userInfo.getClaims()).thenReturn(Map.of("preferred_username",
                "john", "email", "john@example.com"));

        var user = mapper.convert(userSource);

        // The name attribute is only in the userinfo response, so dropping it
        // would both lose claims and fail the login
        assertThat(user.getName()).isEqualTo("john");
        assertThat(user.getUserInfo()).isSameAs(userInfo);
        assertThat(user.getClaims()).containsEntry("email", "john@example.com");
    }

    @Test
    void convert_decoderCreatedOncePerClientRegistration() {
        mapper.convert(userSource);
        mapper.convert(userSource);

        assertThat(decoderFactoryCalls).hasValue(1);
    }

    private List<String> authorities() {
        return mapper.convert(userSource).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();
    }

    private Jwt decode(String tokenValue) {
        var issuedAt = Instant.parse("2026-01-01T00:00:00Z");
        // @formatter:off
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "RS256")
                .issuer(ISSUER_URI)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(60))
                .subject("user-123")
                .claims(claims -> claims.putAll(accessTokenClaims))
                .build();
        // @formatter:on
    }
}
