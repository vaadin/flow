/*
 * Copyright 2000-2019 Vaadin Ltd.
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

package com.vaadin.connect.auth;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

/**
 * Component used for checking role-based ACL in Vaadin Services.
 * <p>
 * For each request that is trying to access the method in the corresponding
 * Vaadin Connect Service, the permission check is carried on.
 * <p>
 * It looks for {@link AnonymousAllowed} {@link PermitAll}, {@link DenyAll} and
 * {@link RolesAllowed} annotations in service methods and classes containing
 * these methods (no super classes' annotations are taken into account).
 * <p>
 * Method-level annotation override Class-level ones.
 * <p>
 * In the next example, since the class is denied to all, method1 is not
 * accessible to anyone, method2 can be executed by any authorized used, method3
 * is only allowed to the accounts having the ROLE_USER authority and method4 is
 * available for every user, including anonymous ones that don't provide any
 * token in their requests.
 *
 * <pre class="code">
 * &#64;VaadinService
 * &#64;DenyAll
 * public class DemoVaadinService {
 *
 *   public void method1() {
 *   }
 *
 *   &#64;PermitAll
 *   public void method2() {
 *   }
 *
 *   &#64;RolesAllowed("ROLE_USER")
 *   public void method3() {
 *   }
 *
 *   &#64;AnonymousAllowed
 *   public void method4() {
 *   }
 * }
 * </pre>
 *
 */
public class VaadinConnectAccessChecker {

  /**
   * Check that the service is accessible for the current user.
   *
   * @param method
   *          the vaadin service method to check ACL
   * @return an error String with an issue description, if any validation issues
   *         occur, {@code null} otherwise
   */
  public String check(Method method) {
    Authentication auth = SecurityContextHolder.getContext()
        .getAuthentication();

    if (auth instanceof OAuth2Authentication) {
      return verifyAuthenticatedUser(method, (OAuth2Authentication) auth);
    } else if (auth instanceof AnonymousAuthenticationToken) {
      return verifyAnonymousUser(method);
    }
    return "Bad authentication, the request should use oauth2";
  }

  /**
   * Gets the entity to check for Vaadin Connect security restrictions.
   *
   * @param method
   *          the method to analyze, not {@code null}
   * @return the entity that is responsible for security settings for the method
   *         passed
   * @throws IllegalArgumentException
   *           if the method is not public
   */
  public AnnotatedElement getSecurityTarget(Method method) {
    if (!Modifier.isPublic(method.getModifiers())) {
      throw new IllegalArgumentException(String.format(
          "The method '%s' is not public hence cannot have a security target",
          method));
    }
    return hasSecurityAnnotation(method) ? method : method.getDeclaringClass();
  }

  private String verifyAnonymousUser(Method method) {
    if (!getSecurityTarget(method).isAnnotationPresent(AnonymousAllowed.class)
        || cannotAccessMethod(method, Collections.emptyList())) {
      return "Anonymous access is not allowed";
    }
    return null;
  }

  private String verifyAuthenticatedUser(Method method,
      OAuth2Authentication auth) {
    if (cannotAccessMethod(method, auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority).collect(Collectors.toList()))) {
      return "Unauthorized access to vaadin service";
    }
    return null;
  }

  private boolean cannotAccessMethod(Method method,
      List<String> requestedAuthorities) {
    return entityForbidden(getSecurityTarget(method), requestedAuthorities);
  }

  private boolean entityForbidden(AnnotatedElement entity,
      List<String> requestedAuthorities) {
    return entity.isAnnotationPresent(DenyAll.class)
        || (!entity.isAnnotationPresent(AnonymousAllowed.class)
            && !roleAllowed(entity.getAnnotation(RolesAllowed.class),
                requestedAuthorities));
  }

  private boolean roleAllowed(RolesAllowed rolesAllowed,
      List<String> requestedAuthorities) {
    if (rolesAllowed == null) {
      return true;
    }

    Set<String> allowedRoles = new HashSet<>(
        Arrays.asList(rolesAllowed.value()));
    return allowedRoles.containsAll(requestedAuthorities);
  }

  private boolean hasSecurityAnnotation(Method method) {
    return method.isAnnotationPresent(AnonymousAllowed.class)
        || method.isAnnotationPresent(PermitAll.class)
        || method.isAnnotationPresent(DenyAll.class)
        || method.isAnnotationPresent(RolesAllowed.class);
  }
}
