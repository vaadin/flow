/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import javax.crypto.SecretKey;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.SecurityContextConfigurer;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.LazyCsrfTokenRepository;
import org.springframework.security.web.savedrequest.CookieRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;

class VaadinStatelessSecurityConfigurer<H extends HttpSecurityBuilder<H>>
        extends
        AbstractHttpConfigurer<VaadinStatelessSecurityConfigurer<H>, H> {
    private long expiresIn = 1800L;

    private String issuer;

    private SecretKeyConfigurer secretKeyConfigurer;

    @Override
    @SuppressWarnings("unchecked")
    public void init(H http) {
        JwtSecurityContextRepository jwtSecurityContextRepository = new JwtSecurityContextRepository();
        SecurityContextConfigurer<H> securityContext = http
                .getConfigurer(SecurityContextConfigurer.class);
        if (securityContext != null) {
            securityContext
                    .securityContextRepository(jwtSecurityContextRepository);
        } else {
            http.setSharedObject(SecurityContextRepository.class,
                    jwtSecurityContextRepository);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void configure(H http) {
        SecurityContextRepository securityContextRepository = http
                .getSharedObject(SecurityContextRepository.class);

        if (securityContextRepository instanceof JwtSecurityContextRepository) {
            JwtSecurityContextRepository jwtSecurityContextRepository = (JwtSecurityContextRepository) securityContextRepository;

            jwtSecurityContextRepository
                    .setJwsAlgorithm(secretKeyConfigurer.getAlgorithm());
            jwtSecurityContextRepository
                    .setJwkSource(secretKeyConfigurer.getJWKSource());
            jwtSecurityContextRepository.setIssuer(issuer);
            jwtSecurityContextRepository.setExpiresIn(expiresIn);

            AuthenticationTrustResolver trustResolver = http
                    .getSharedObject(AuthenticationTrustResolver.class);
            if (trustResolver == null) {
                trustResolver = new AuthenticationTrustResolverImpl();
            }
            jwtSecurityContextRepository.setTrustResolver(trustResolver);
        }

        RequestCache requestCache = http.getSharedObject(RequestCache.class);
        if (requestCache instanceof VaadinDefaultRequestCache) {
            ((VaadinDefaultRequestCache) requestCache)
                    .setDelegateRequestCache(new CookieRequestCache());
        }

        CsrfConfigurer<H> csrf = http.getConfigurer(CsrfConfigurer.class);
        if (csrf != null) {
            // Use cookie for storing CSRF token, as it does not require a
            // session (double-submit cookie pattern)
            CsrfTokenRepository csrfTokenRepository = new LazyCsrfTokenRepository(
                    CookieCsrfTokenRepository.withHttpOnlyFalse());
            csrf.csrfTokenRepository(csrfTokenRepository);
        }
    }

    public VaadinStatelessSecurityConfigurer<H> expiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
        return this;
    }

    public VaadinStatelessSecurityConfigurer<H> issuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public SecretKeyConfigurer withSecretKey() {
        if (this.secretKeyConfigurer == null) {
            this.secretKeyConfigurer = new SecretKeyConfigurer();
        }
        return this.secretKeyConfigurer;
    }

    public SecretKeyConfigurer withSecretKey(
            Customizer<SecretKeyConfigurer> customizer) {
        if (this.secretKeyConfigurer == null) {
            this.secretKeyConfigurer = new SecretKeyConfigurer();
        }
        customizer.customize(secretKeyConfigurer);
        return this.secretKeyConfigurer;
    }

    public class SecretKeyConfigurer {
        private SecretKey secretKey;

        private JwsAlgorithm jwsAlgorithm;

        private SecretKeyConfigurer() {
        }

        public SecretKeyConfigurer secretKey(SecretKey secretKey) {
            this.secretKey = secretKey;
            if (this.jwsAlgorithm == null) {
                this.jwsAlgorithm = MacAlgorithm.from(secretKey.getAlgorithm());
            }
            return this;
        }

        public SecretKeyConfigurer algorithm(MacAlgorithm algorithm) {
            this.jwsAlgorithm = algorithm;
            return this;
        }

        public VaadinStatelessSecurityConfigurer<H> and() {
            return VaadinStatelessSecurityConfigurer.this;
        }

        JWKSource<SecurityContext> getJWKSource() {
            OctetSequenceKey key = new OctetSequenceKey.Builder(secretKey)
                    .algorithm(getAlgorithm()).build();
            JWKSet jwkSet = new JWKSet(key);
            return (jwkSelector, context) -> jwkSelector.select(jwkSet);
        }

        JWSAlgorithm getAlgorithm() {
            return JWSAlgorithm.parse(jwsAlgorithm.getName());
        }
    }
}
