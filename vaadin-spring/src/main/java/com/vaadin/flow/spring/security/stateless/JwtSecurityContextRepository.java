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
package com.vaadin.flow.spring.security.stateless;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * A {@link SecurityContextRepository} implementation that stores the
 * authentication using a JWT persisted in cookies.
 */
class JwtSecurityContextRepository implements SecurityContextRepository {
    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_AUTHORITY_PREFIX = "ROLE_";
    private final Log logger = LogFactory.getLog(this.getClass());
    private final SerializedJwtSplitCookieRepository serializedJwtSplitCookieRepository;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private String issuer;
    private long expiresIn = 1800L;
    private JWKSource<com.nimbusds.jose.proc.SecurityContext> jwkSource;
    private JWSAlgorithm jwsAlgorithm;
    private JwtDecoder jwtDecoder;
    private AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    JwtSecurityContextRepository(
            SerializedJwtSplitCookieRepository serializedJwtSplitCookieRepository) {
        this.serializedJwtSplitCookieRepository = serializedJwtSplitCookieRepository;
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix(ROLE_AUTHORITY_PREFIX);
        grantedAuthoritiesConverter.setAuthoritiesClaimName(ROLES_CLAIM);

        jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter
                .setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
    }

    void setJwkSource(
            JWKSource<com.nimbusds.jose.proc.SecurityContext> jwkSource) {
        this.jwkSource = jwkSource;
    }

    void setJwsAlgorithm(JWSAlgorithm jwsAlgorithm) {
        this.jwsAlgorithm = jwsAlgorithm;
    }

    void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
        this.serializedJwtSplitCookieRepository.setExpiresIn(expiresIn);
    }

    void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    void setTrustResolver(AuthenticationTrustResolver trustResolver) {
        this.trustResolver = trustResolver;
    }

    private JwtDecoder getJwtDecoder() {
        if (jwtDecoder != null) {
            return jwtDecoder;
        }

        DefaultJWTProcessor<com.nimbusds.jose.proc.SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWTClaimsSetVerifier((claimsSet, context) -> {
            // No-op, Spring Security’s NimbusJwtDecoder uses its own validator
        });

        JWSKeySelector<com.nimbusds.jose.proc.SecurityContext> jwsKeySelector = new JWSVerificationKeySelector<>(
                jwsAlgorithm, jwkSource);
        jwtProcessor.setJWSKeySelector(jwsKeySelector);
        NimbusJwtDecoder nimbusJwtDecoder = new NimbusJwtDecoder(jwtProcessor);
        nimbusJwtDecoder.setJwtValidator(
                issuer != null ? JwtValidators.createDefaultWithIssuer(issuer)
                        : JwtValidators.createDefault());
        this.jwtDecoder = nimbusJwtDecoder;
        return jwtDecoder;
    }

    private String encodeJwt(Authentication authentication)
            throws JOSEException {
        if (authentication == null
                || trustResolver.isAnonymous(authentication)) {
            return null;
        }

        final Date now = new Date();

        final List<String> roles = authentication.getAuthorities().stream()
                .map(Objects::toString)
                .filter(a -> a.startsWith(ROLE_AUTHORITY_PREFIX))
                .map(a -> a.substring(ROLE_AUTHORITY_PREFIX.length()))
                .collect(Collectors.toList());

        SignedJWT signedJWT;
        JWSHeader jwsHeader = new JWSHeader(jwsAlgorithm);
        JWKSelector jwkSelector = new JWKSelector(
                JWKMatcher.forJWSHeader(jwsHeader));

        List<JWK> jwks = jwkSource.get(jwkSelector, null);
        JWK jwk = jwks.get(0);

        JWSSigner signer = new DefaultJWSSignerFactory().createJWSSigner(jwk,
                jwsAlgorithm);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(authentication.getName()).issuer(issuer).issueTime(now)
                .expirationTime(new Date(now.getTime() + expiresIn * 1000))
                .claim(ROLES_CLAIM, roles).build();
        signedJWT = new SignedJWT(jwsHeader, claimsSet);
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

    private Jwt decodeJwt(HttpServletRequest request) {
        String serializedJwt = serializedJwtSplitCookieRepository
                .loadSerializedJwt(request);
        if (serializedJwt == null) {
            return null;
        }

        try {
            return getJwtDecoder().decode(serializedJwt);
        } catch (JwtException e) {
            if (this.logger.isTraceEnabled()) {
                this.logger.trace(
                        "Cannot decode JWT when loading SecurityContext", e);
            }
            return null;
        }
    }

    @Override
    public SecurityContext loadContext(
            HttpRequestResponseHolder requestResponseHolder) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        HttpServletRequest request = requestResponseHolder.getRequest();

        Jwt jwt = decodeJwt(request);
        if (jwt != null) {
            Authentication authentication = jwtAuthenticationConverter
                    .convert(jwt);
            context.setAuthentication(authentication);
        }
        return context;
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request,
            HttpServletResponse response) {
        String serializedJwt = null;
        try {
            serializedJwt = encodeJwt(context.getAuthentication());
        } catch (JOSEException e) {
            logger.warn("Cannot serialize SecurityContext as JWT", e);
        } finally {
            serializedJwtSplitCookieRepository.saveSerializedJwt(serializedJwt,
                    request, response);
        }
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        return serializedJwtSplitCookieRepository
                .containsSerializedJwt(request);
    }
}
