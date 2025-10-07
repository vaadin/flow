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

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTProcessor;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class JwtSecurityContextRepositoryTest {
    static private final String TEST_USERNAME = "username@example.com";
    static private final String TEST_ISSUER = "https://app.example.com";
    static private final String TEST_OTHER_ISSUER = "https://other.example.com";
    static private final ArrayList<String> TEST_ROLES = Lists
            .newArrayList("user", "employee");
    static private final Collection<? extends GrantedAuthority> TEST_AUTHORITIES = TEST_ROLES
            .stream().map(role -> "ROLE_" + role)
            .map(SimpleGrantedAuthority::new).collect(Collectors.toList());

    static private final SecretKey TEST_KEY = new SecretKeySpec(
            Base64.getDecoder()
                    .decode("bu2oBxH0Kh5Hx4c1dJMZ+VgvUQJQe708ODGz/6TfYYw="),
            MacAlgorithm.HS256.getName());
    static private final SecretKey TEST_OTHER_KEY = new SecretKeySpec(
            Base64.getDecoder()
                    .decode("n7pNtBaK8PaIH1UKOsJTAEbfroSVfPxjLfpd9qHzg6Y="),
            MacAlgorithm.HS256.getName());
    static private final SecretKey TEST_64BYTE_KEY = new SecretKeySpec(
            Base64.getDecoder().decode(
                    "0qqXt2AICntpoRfIR4pP0DgXi0tmELlvhwqBEoxaMR+bQDlibp6WhrakeqFyT9SquWsKFtcA37LgAIF6znoDLQ=="),
            MacAlgorithm.HS512.getName());

    static private final Instant TEST_PAST = Instant.EPOCH;
    static private final Instant TEST_FUTURE = Instant
            .ofEpochSecond(0x7fffffffffL);

    static private DefaultJWTProcessor<?> jwtProcessor;

    private JwtSecurityContextRepository jwtSecurityContextRepository;

    @Mock
    private SerializedJwtSplitCookieRepository serializedJwtSplitCookieRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;
    private SecurityContextHolderStrategy originalontextHolderStrategy;

    private static JWTClaimsSet.Builder getClaimsSetBuilder() {
        return new JWTClaimsSet.Builder().issueTime(Date.from(TEST_PAST))
                .expirationTime(Date.from(TEST_FUTURE)).subject(TEST_USERNAME)
                .claim("roles", TEST_ROLES);
    }

    private static JWSHeader.Builder getHeaderBuilder() {
        return new JWSHeader.Builder(JWSAlgorithm.HS256)
                .type(JOSEObjectType.JWT);
    }

    private static JWTClaimsSet decodeSerializedJwt(String serializedJwt,
            JWTProcessor<?> jwtProcessor)
            throws BadJOSEException, ParseException, JOSEException {
        Assert.assertNotNull(serializedJwt);
        Assert.assertNotEquals("", serializedJwt);
        return jwtProcessor.process(serializedJwt, null);
    }

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.openMocks(this);

        originalontextHolderStrategy = SecurityContextHolder
                .getContextHolderStrategy();
        SecurityContextHolder.setStrategyName(
                TestSecurityContextHolderStrategy.class.getName());

        final ImmutableSecret secret = new ImmutableSecret<>(TEST_KEY);

        jwtSecurityContextRepository = new JwtSecurityContextRepository(
                serializedJwtSplitCookieRepository);
        jwtSecurityContextRepository.setJwkSource(secret);
        jwtSecurityContextRepository.setJwsAlgorithm(JWSAlgorithm.HS256);
        // Processor for asserting saved JWTs
        jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWSKeySelector(
                new JWSVerificationKeySelector<>(JWSAlgorithm.HS256, secret));
    }

    @After
    public void teardown() {
        Mockito.verifyNoInteractions(request);
        Mockito.verifyNoInteractions(response);
        if (originalontextHolderStrategy != null) {
            SecurityContextHolder
                    .setContextHolderStrategy(originalontextHolderStrategy);
        } else {
            SecurityContextHolder.setStrategyName(null);
        }
    }

    @Test
    public void loadContext_returnsEmptyContext_when_nullJwt() {
        SecurityContext securityContext = jwtSecurityContextRepository
                .loadDeferredContext(request).get();

        Mockito.verify(serializedJwtSplitCookieRepository)
                .loadSerializedJwt(request);

        assertEmptySecurityContext(securityContext);
    }

    @Test
    public void loadContext_returnsEmptyContext_when_plainJwt() {
        Mockito.doReturn(
                new PlainJWT(getClaimsSetBuilder().build()).serialize())
                .when(serializedJwtSplitCookieRepository)
                .loadSerializedJwt(request);

        SecurityContext securityContext = jwtSecurityContextRepository
                .loadDeferredContext(request).get();

        assertEmptySecurityContext(securityContext);
    }

    @Test
    public void loadContext_returnsEmptyContext_when_keySourceReturnsNull()
            throws JOSEException {
        Mockito.doReturn(getJwt(getHeaderBuilder().build(),
                getClaimsSetBuilder().build()))
                .when(serializedJwtSplitCookieRepository)
                .loadSerializedJwt(request);
        jwtSecurityContextRepository
                .setJwkSource((jwkSelector, context) -> null);

        SecurityContext securityContext = jwtSecurityContextRepository
                .loadDeferredContext(request).get();

        assertEmptySecurityContext(securityContext);
    }

    @Test
    public void loadContext_returnsEmptyContext_when_keySourceReturnsEmpty()
            throws JOSEException {
        Mockito.doReturn(getJwt(getHeaderBuilder().build(),
                getClaimsSetBuilder().build()))
                .when(serializedJwtSplitCookieRepository)
                .loadSerializedJwt(request);
        jwtSecurityContextRepository
                .setJwkSource((jwkSelector, context) -> new ArrayList<>());

        SecurityContext securityContext = jwtSecurityContextRepository
                .loadDeferredContext(request).get();

        assertEmptySecurityContext(securityContext);
    }

    @Test
    public void loadContext_returnsEmptyContext_when_jwtSignatureMissing()
            throws JOSEException {
        String missingSignatureJwt = getJwt(getHeaderBuilder().build(),
                getClaimsSetBuilder().build()).replaceFirst("\\.[^.]*$", ".");
        Mockito.doReturn(missingSignatureJwt)
                .when(serializedJwtSplitCookieRepository)
                .loadSerializedJwt(request);

        SecurityContext securityContext = jwtSecurityContextRepository
                .loadDeferredContext(request).get();

        assertEmptySecurityContext(securityContext);
    }

    @Test
    public void loadContext_returnsEmptyContext_when_jwtSignatureInvalid()
            throws JOSEException {
        String invalidSignatureJwt = getJwt(getHeaderBuilder().build(),
                getClaimsSetBuilder().build(), new MACSigner(TEST_OTHER_KEY));
        Mockito.doReturn(invalidSignatureJwt)
                .when(serializedJwtSplitCookieRepository)
                .loadSerializedJwt(request);

        SecurityContext securityContext = jwtSecurityContextRepository
                .loadDeferredContext(request).get();

        assertEmptySecurityContext(securityContext);
    }

    @Test
    public void loadContext_returnsEmptyContext_when_expiredJwt()
            throws JOSEException {
        String missingSignatureJwt = getJwt(getHeaderBuilder().build(),
                getClaimsSetBuilder().expirationTime(Date.from(TEST_PAST))
                        .build());
        Mockito.doReturn(missingSignatureJwt)
                .when(serializedJwtSplitCookieRepository)
                .loadSerializedJwt(request);

        SecurityContext securityContext = jwtSecurityContextRepository
                .loadDeferredContext(request).get();

        assertEmptySecurityContext(securityContext);
    }

    @Test
    public void loadContext_returnsEmptyContext_when_jwtIssuerMissing()
            throws JOSEException {
        jwtSecurityContextRepository.setIssuer(TEST_ISSUER);
        Mockito.doReturn(getJwt(getHeaderBuilder().build(),
                getClaimsSetBuilder().build()))
                .when(serializedJwtSplitCookieRepository)
                .loadSerializedJwt(request);

        SecurityContext securityContext = jwtSecurityContextRepository
                .loadDeferredContext(request).get();

        assertEmptySecurityContext(securityContext);
    }

    @Test
    public void loadContext_returnsEmptyContext_when_jwtIssuerInvalid()
            throws JOSEException {
        jwtSecurityContextRepository.setIssuer(TEST_ISSUER);
        Mockito.doReturn(getJwt(getHeaderBuilder().build(),
                getClaimsSetBuilder().issuer(TEST_OTHER_ISSUER).build()))
                .when(serializedJwtSplitCookieRepository)
                .loadSerializedJwt(request);

        SecurityContext securityContext = jwtSecurityContextRepository
                .loadDeferredContext(request).get();

        assertEmptySecurityContext(securityContext);
    }

    @Test
    public void loadContext_returnsEmptyContext_when_algorithmHasNoKey()
            throws JOSEException {
        Mockito.doReturn(getJwt(getHeaderBuilder().build(),
                getClaimsSetBuilder().issuer(TEST_OTHER_ISSUER).build()))
                .when(serializedJwtSplitCookieRepository)
                .loadSerializedJwt(request);
        jwtSecurityContextRepository.setJwsAlgorithm(JWSAlgorithm.HS512);

        SecurityContext securityContext = jwtSecurityContextRepository
                .loadDeferredContext(request).get();

        assertEmptySecurityContext(securityContext);
    }

    @Test
    public void loadContext_returnsUserContext_when_validJwt_withoutIssuer()
            throws JOSEException {
        Mockito.doReturn(getJwt(getHeaderBuilder().build(),
                getClaimsSetBuilder().build()))
                .when(serializedJwtSplitCookieRepository)
                .loadSerializedJwt(request);

        SecurityContext securityContext = jwtSecurityContextRepository
                .loadDeferredContext(request).get();

        assertSecurityContext(TEST_USERNAME, TEST_AUTHORITIES, securityContext);
    }

    @Test
    public void loadContext_returnsUserContext_when_validJwt_withIssuer()
            throws JOSEException {
        jwtSecurityContextRepository.setIssuer(TEST_ISSUER);
        Mockito.doReturn(getJwt(getHeaderBuilder().build(),
                getClaimsSetBuilder().issuer(TEST_ISSUER).build()))
                .when(serializedJwtSplitCookieRepository)
                .loadSerializedJwt(request);

        SecurityContext securityContext = jwtSecurityContextRepository
                .loadDeferredContext(request).get();

        assertSecurityContext(TEST_USERNAME, TEST_AUTHORITIES, securityContext);
    }

    @Test
    public void loadContext_throws_when_algorithmNull() throws JOSEException {
        jwtSecurityContextRepository.setIssuer(TEST_ISSUER);
        Mockito.doReturn(getJwt(getHeaderBuilder().build(),
                getClaimsSetBuilder().issuer(TEST_OTHER_ISSUER).build()))
                .when(serializedJwtSplitCookieRepository)
                .loadSerializedJwt(request);
        jwtSecurityContextRepository.setJwsAlgorithm(null);

        Assert.assertThrows(NullPointerException.class,
                () -> jwtSecurityContextRepository.loadDeferredContext(request)
                        .get());
    }

    @Test
    public void loadContext_throws_when_keySourceNull() throws JOSEException {
        Mockito.doReturn(getJwt(getHeaderBuilder().build(),
                getClaimsSetBuilder().build()))
                .when(serializedJwtSplitCookieRepository)
                .loadSerializedJwt(request);
        jwtSecurityContextRepository.setJwkSource(null);

        Assert.assertThrows(NullPointerException.class,
                () -> jwtSecurityContextRepository.loadDeferredContext(request)
                        .get());
    }

    @Test
    public void containsContext_returnsFalse_when_noJwtInRepository() {
        Mockito.doReturn(false).when(serializedJwtSplitCookieRepository)
                .containsSerializedJwt(request);

        boolean contains = jwtSecurityContextRepository
                .containsContext(request);

        Assert.assertFalse(contains);
        Mockito.verify(serializedJwtSplitCookieRepository)
                .containsSerializedJwt(request);
        Mockito.verifyNoInteractions(request);
    }

    @Test
    public void containsContext_returnsTrue_when_jwtInRepository() {
        Mockito.doReturn(true).when(serializedJwtSplitCookieRepository)
                .containsSerializedJwt(request);

        boolean contains = jwtSecurityContextRepository
                .containsContext(request);

        Assert.assertTrue(contains);
        Mockito.verify(serializedJwtSplitCookieRepository)
                .containsSerializedJwt(request);
    }

    @Test
    public void saveContext_doesNotSaveJwt_when_securityContextEmpty() {
        SecurityContext emptyContext = SecurityContextHolder
                .createEmptyContext();

        jwtSecurityContextRepository.saveContext(emptyContext, request,
                response);

        String serializedJwt = getSavedSerializedJwt();
        Assert.assertNull(serializedJwt);
    }

    @Test
    public void saveContext_doesNotSaveJwt_when_securityContextAnonymous() {
        AnonymousAuthenticationToken anonymousAuthenticationToken = new AnonymousAuthenticationToken(
                "key", "anonymous",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        SecurityContext securityContext = SecurityContextHolder
                .createEmptyContext();
        Mockito.doReturn(anonymousAuthenticationToken).when(securityContext)
                .getAuthentication();

        jwtSecurityContextRepository.saveContext(securityContext, request,
                response);

        String serializedJwt = getSavedSerializedJwt();
        Assert.assertNull(serializedJwt);
    }

    @Test
    public void saveContext_doesNotSaveJwt_when_trustResolverIsAnonymousReturnsTrue()
            throws JOSEException {
        SecurityContext securityContext = SecurityContextHolder
                .createEmptyContext();
        JWSHeader header = getHeaderBuilder().build();
        JWTClaimsSet claimsSet = getClaimsSetBuilder().build();
        Authentication authentication = getJwtAuthenticationToken(header,
                claimsSet);
        Mockito.doReturn(authentication).when(securityContext)
                .getAuthentication();
        AuthenticationTrustResolver trustResolver = Mockito
                .mock(AuthenticationTrustResolver.class);
        Mockito.doReturn(true).when(trustResolver).isAnonymous(authentication);
        jwtSecurityContextRepository.setTrustResolver(trustResolver);

        jwtSecurityContextRepository.saveContext(securityContext, request,
                response);

        Mockito.verify(trustResolver).isAnonymous(authentication);
        String serializedJwt = getSavedSerializedJwt();
        Assert.assertNull(serializedJwt);
    }

    @Test
    public void saveContext_doesNotSaveJwt_when_algorithmHasNoKey()
            throws JOSEException {
        SecurityContext securityContext = SecurityContextHolder
                .createEmptyContext();
        JWSHeader header = getHeaderBuilder().build();
        JWTClaimsSet claimsSet = getClaimsSetBuilder().build();
        Mockito.doReturn(getJwtAuthenticationToken(header, claimsSet))
                .when(securityContext).getAuthentication();
        jwtSecurityContextRepository.setJwsAlgorithm(JWSAlgorithm.HS512);

        jwtSecurityContextRepository.saveContext(securityContext, request,
                response);

        String serializedJwt = getSavedSerializedJwt();
        Assert.assertNull(serializedJwt);
    }

    @Test
    public void saveContext_doesNotSaveJwt_when_keySourceNull()
            throws JOSEException {
        SecurityContext securityContext = SecurityContextHolder
                .createEmptyContext();
        JWSHeader header = getHeaderBuilder().build();
        JWTClaimsSet claimsSet = getClaimsSetBuilder().build();
        Mockito.doReturn(getJwtAuthenticationToken(header, claimsSet))
                .when(securityContext).getAuthentication();
        jwtSecurityContextRepository.setJwkSource(null);

        Assert.assertThrows(NullPointerException.class,
                () -> jwtSecurityContextRepository.saveContext(securityContext,
                        request, response));

        String serializedJwt = getSavedSerializedJwt();
        Assert.assertNull(serializedJwt);
    }

    @Test
    public void saveContext_doesNotSaveJwt_when_keySourceReturnsNull()
            throws JOSEException {
        SecurityContext securityContext = SecurityContextHolder
                .createEmptyContext();
        JWSHeader header = getHeaderBuilder().build();
        JWTClaimsSet claimsSet = getClaimsSetBuilder().build();
        Mockito.doReturn(getJwtAuthenticationToken(header, claimsSet))
                .when(securityContext).getAuthentication();
        jwtSecurityContextRepository
                .setJwkSource((jwkSelector, context) -> null);

        Assert.assertThrows(NullPointerException.class,
                () -> jwtSecurityContextRepository.saveContext(securityContext,
                        request, response));

        String serializedJwt = getSavedSerializedJwt();
        Assert.assertNull(serializedJwt);
    }

    @Test
    public void saveContext_doesNotSaveJwt_when_keySourceReturnsEmpty()
            throws JOSEException {
        SecurityContext securityContext = SecurityContextHolder
                .createEmptyContext();
        JWSHeader header = getHeaderBuilder().build();
        JWTClaimsSet claimsSet = getClaimsSetBuilder().build();
        Mockito.doReturn(getJwtAuthenticationToken(header, claimsSet))
                .when(securityContext).getAuthentication();
        jwtSecurityContextRepository
                .setJwkSource((jwkSelector, context) -> new ArrayList<>());

        Assert.assertThrows(IndexOutOfBoundsException.class,
                () -> jwtSecurityContextRepository.saveContext(securityContext,
                        request, response));

        String serializedJwt = getSavedSerializedJwt();
        Assert.assertNull(serializedJwt);
    }

    @Test
    public void saveContext_doesNotSaveJwt_when_algorithmNull()
            throws JOSEException {
        SecurityContext securityContext = SecurityContextHolder
                .createEmptyContext();
        JWSHeader header = getHeaderBuilder().build();
        JWTClaimsSet claimsSet = getClaimsSetBuilder().build();
        Mockito.doReturn(getJwtAuthenticationToken(header, claimsSet))
                .when(securityContext).getAuthentication();
        jwtSecurityContextRepository.setJwsAlgorithm(null);

        Assert.assertThrows(NullPointerException.class,
                () -> jwtSecurityContextRepository.saveContext(securityContext,
                        request, response));

        String serializedJwt = getSavedSerializedJwt();
        Assert.assertNull(serializedJwt);
    }

    @Test
    public void saveContext_doesSaveJwt_when_givenNonJwtContext()
            throws JOSEException {
        SecurityContext securityContext = SecurityContextHolder
                .createEmptyContext();
        User testUser = new User(TEST_USERNAME, "", TEST_AUTHORITIES);
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                testUser, testUser.getPassword(), testUser.getAuthorities());
        Mockito.doReturn(usernamePasswordAuthenticationToken)
                .when(securityContext).getAuthentication();

        jwtSecurityContextRepository.saveContext(securityContext, request,
                response);

        String serializedJwt = getSavedSerializedJwt();
        Assert.assertNotNull(serializedJwt);
        Assert.assertNotEquals("", serializedJwt);
    }

    @Test
    public void saveContext_doesSaveJwt_when_givenJwtContext()
            throws JOSEException, BadJOSEException, ParseException {
        SecurityContext securityContext = SecurityContextHolder
                .createEmptyContext();
        JWSHeader header = getHeaderBuilder().build();
        JWTClaimsSet claimsSet = getClaimsSetBuilder().build();
        Mockito.doReturn(getJwtAuthenticationToken(header, claimsSet))
                .when(securityContext).getAuthentication();

        jwtSecurityContextRepository.saveContext(securityContext, request,
                response);

        String serializedJwt = getSavedSerializedJwt();
        JWTClaimsSet decodedClaimsSet = decodeSerializedJwt(serializedJwt,
                jwtProcessor);
        assertClaims(decodedClaimsSet, TEST_USERNAME, TEST_ROLES, 1800);
        Assert.assertEquals(null, decodedClaimsSet.getIssuer());
    }

    @Test
    public void saveContext_doesSaveJwt_when_trustResolverIsAnonymousReturnsFalse()
            throws JOSEException, BadJOSEException, ParseException {
        AnonymousAuthenticationToken anonymousAuthenticationToken = new AnonymousAuthenticationToken(
                "key", "anonymous",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        SecurityContext securityContext = SecurityContextHolder
                .createEmptyContext();
        Mockito.doReturn(anonymousAuthenticationToken).when(securityContext)
                .getAuthentication();
        AuthenticationTrustResolver trustResolver = Mockito
                .mock(AuthenticationTrustResolver.class);
        Mockito.doReturn(false).when(trustResolver)
                .isAnonymous(anonymousAuthenticationToken);
        jwtSecurityContextRepository.setTrustResolver(trustResolver);

        jwtSecurityContextRepository.saveContext(securityContext, request,
                response);

        String serializedJwt = getSavedSerializedJwt();
        JWTClaimsSet decodedClaimsSet = decodeSerializedJwt(serializedJwt,
                jwtProcessor);

        assertClaims(decodedClaimsSet, "anonymous",
                Lists.newArrayList("ANONYMOUS"), 1800);
        Assert.assertEquals(null, decodedClaimsSet.getIssuer());
    }

    @Test
    public void saveContext_doesSaveJwt_withIssuer()
            throws JOSEException, BadJOSEException, ParseException {
        SecurityContext securityContext = SecurityContextHolder
                .createEmptyContext();
        JWSHeader header = getHeaderBuilder().build();
        JWTClaimsSet claimsSet = getClaimsSetBuilder().build();
        Mockito.doReturn(getJwtAuthenticationToken(header, claimsSet))
                .when(securityContext).getAuthentication();
        jwtSecurityContextRepository.setIssuer(TEST_ISSUER);

        jwtSecurityContextRepository.saveContext(securityContext, request,
                response);

        String serializedJwt = getSavedSerializedJwt();
        JWTClaimsSet decodedClaimsSet = decodeSerializedJwt(serializedJwt,
                jwtProcessor);
        assertClaims(decodedClaimsSet, TEST_USERNAME, TEST_ROLES, 1800);
        Assert.assertEquals(TEST_ISSUER, decodedClaimsSet.getIssuer());
    }

    @Test
    public void saveContext_doesSaveJwt_withExpiresIn()
            throws JOSEException, BadJOSEException, ParseException {
        SecurityContext securityContext = SecurityContextHolder
                .createEmptyContext();
        JWSHeader header = getHeaderBuilder().build();
        JWTClaimsSet claimsSet = getClaimsSetBuilder().build();
        Mockito.doReturn(getJwtAuthenticationToken(header, claimsSet))
                .when(securityContext).getAuthentication();
        jwtSecurityContextRepository.setExpiresIn(300);

        jwtSecurityContextRepository.saveContext(securityContext, request,
                response);

        String serializedJwt = getSavedSerializedJwt();
        JWTClaimsSet decodedClaimsSet = decodeSerializedJwt(serializedJwt,
                jwtProcessor);
        assertClaims(decodedClaimsSet, TEST_USERNAME, TEST_ROLES, 300);
    }

    @Test
    public void saveContext_doesSaveJwt_withOtherKey()
            throws JOSEException, BadJOSEException, ParseException {
        SecurityContext securityContext = SecurityContextHolder
                .createEmptyContext();
        JWSHeader header = getHeaderBuilder().build();
        JWTClaimsSet claimsSet = getClaimsSetBuilder().build();
        Mockito.doReturn(getJwtAuthenticationToken(header, claimsSet))
                .when(securityContext).getAuthentication();
        final ImmutableSecret secret = new ImmutableSecret<>(TEST_OTHER_KEY);
        jwtSecurityContextRepository.setJwkSource(secret);

        jwtSecurityContextRepository.saveContext(securityContext, request,
                response);

        jwtProcessor.setJWSKeySelector(
                new JWSVerificationKeySelector<>(JWSAlgorithm.HS256, secret));
        String serializedJwt = getSavedSerializedJwt();
        JWTClaimsSet decodedClaimsSet = decodeSerializedJwt(serializedJwt,
                jwtProcessor);
        assertClaims(decodedClaimsSet, TEST_USERNAME, TEST_ROLES, 1800);
    }

    @Test
    public void saveContext_doesSaveJwt_withAlgoritm()
            throws JOSEException, BadJOSEException, ParseException {
        SecurityContext securityContext = SecurityContextHolder
                .createEmptyContext();
        JWSHeader header = getHeaderBuilder().build();
        JWTClaimsSet claimsSet = getClaimsSetBuilder().build();
        Mockito.doReturn(getJwtAuthenticationToken(header, claimsSet))
                .when(securityContext).getAuthentication();
        final ImmutableSecret secret = new ImmutableSecret<>(
                TEST_64BYTE_KEY.getEncoded());
        jwtSecurityContextRepository.setJwkSource(secret);
        jwtSecurityContextRepository.setJwsAlgorithm(JWSAlgorithm.HS512);

        jwtSecurityContextRepository.saveContext(securityContext, request,
                response);

        jwtProcessor.setJWSKeySelector(
                new JWSVerificationKeySelector<>(JWSAlgorithm.HS512, secret));
        String serializedJwt = getSavedSerializedJwt();
        JWTClaimsSet decodedClaimsSet = decodeSerializedJwt(serializedJwt,
                jwtProcessor);
        assertClaims(decodedClaimsSet, TEST_USERNAME, TEST_ROLES, 1800);
    }

    private void assertEmptySecurityContext(SecurityContext securityContext) {
        Mockito.verifyNoInteractions(securityContext);
    }

    private void assertSecurityContext(String username,
            Collection<? extends GrantedAuthority> authorities,
            SecurityContext securityContext) throws JOSEException {
        Mockito.verify(serializedJwtSplitCookieRepository)
                .loadSerializedJwt(request);
        ArgumentCaptor<JwtAuthenticationToken> captor = ArgumentCaptor
                .forClass(JwtAuthenticationToken.class);
        Mockito.verify(securityContext).setAuthentication(captor.capture());
        JwtAuthenticationToken actualAuthentication = captor.getValue();
        Assert.assertTrue(actualAuthentication.isAuthenticated());
        Assert.assertEquals(username, actualAuthentication.getName());
        Assert.assertEquals(authorities, actualAuthentication.getAuthorities());
    }

    private void assertClaims(JWTClaimsSet claimsSet, String username,
            ArrayList<String> roles, long expiresIn) {
        Assert.assertEquals(username, claimsSet.getSubject());
        Assert.assertEquals(roles, claimsSet.getClaim("roles"));
        Assert.assertTrue(
                Instant.now().isAfter(claimsSet.getIssueTime().toInstant()));
        final Duration lag = Duration.ofSeconds(10);
        final Instant iat = claimsSet.getIssueTime().toInstant();
        final Instant iatEstimate = Instant.now();
        Assert.assertTrue(iatEstimate.minus(lag).isBefore(iat));
        Assert.assertTrue(iatEstimate.plus(lag).isAfter(iat));
        final Instant exp = claimsSet.getExpirationTime().toInstant();
        final Instant expEstimate = iatEstimate
                .plus(Duration.ofSeconds(expiresIn));
        Assert.assertTrue(expEstimate.minus(lag).isBefore(exp));
        Assert.assertTrue(expEstimate.plus(lag).isAfter(exp));
    }

    private String getSavedSerializedJwt() {
        ArgumentCaptor<String> jwtCaptor = ArgumentCaptor
                .forClass(String.class);
        Mockito.verify(serializedJwtSplitCookieRepository).saveSerializedJwt(
                jwtCaptor.capture(), ArgumentMatchers.eq(request),
                ArgumentMatchers.eq(response));
        String serializedJwt = jwtCaptor.getValue();
        return serializedJwt;
    }

    private String getJwt(JWSHeader header, JWTClaimsSet claimsSet)
            throws JOSEException {
        return getJwt(header, claimsSet, new MACSigner(TEST_KEY));
    }

    private String getJwt(JWSHeader header, JWTClaimsSet claimsSet,
            JWSSigner signer) throws JOSEException {
        SignedJWT jwt = new SignedJWT(header, claimsSet);
        jwt.sign(signer);
        return jwt.serialize();
    }

    private JwtAuthenticationToken getJwtAuthenticationToken(JWSHeader header,
            JWTClaimsSet claimsSet) throws JOSEException {
        Jwt jwt = new Jwt(getJwt(header, claimsSet),
                claimsSet.getIssueTime().toInstant(),
                claimsSet.getExpirationTime().toInstant(),
                header.toJSONObject(), claimsSet.toJSONObject());
        return new JwtAuthenticationToken(jwt, TEST_AUTHORITIES);
    }
}
