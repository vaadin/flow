/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.cdi;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.vaadin.cdi.DeploymentValidator.DeploymentProblem.ErrorCode;
import com.vaadin.cdi.annotation.NormalRouteScoped;
import com.vaadin.cdi.annotation.RouteScopeOwner;
import com.vaadin.cdi.annotation.RouteScoped;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

import static com.vaadin.cdi.DeploymentValidator.DeploymentProblem.ErrorCode.NON_ROUTE_SCOPED_HAVE_OWNER;
import static com.vaadin.cdi.DeploymentValidator.DeploymentProblem.ErrorCode.NORMAL_SCOPED_COMPONENT;
import static com.vaadin.cdi.DeploymentValidator.DeploymentProblem.ErrorCode.OWNER_IS_NOT_ROUTE_COMPONENT;

@Dependent
class DeploymentValidator {

    static class BeanInfo {

        private final Bean<?> bean;
        private final Annotated annotated;

        BeanInfo(Bean<?> bean, Annotated annotated) {
            this.bean = bean;
            this.annotated = annotated;
        }

        Type getBaseType() {
            return annotated.getBaseType();
        }

        private boolean isNormalScoped(BeanManager bm) {
            return bm.isNormalScope(bean.getScope());
        }

        private boolean isRouteScoped() {
            Class<? extends Annotation> scope = bean.getScope();
            return scope.equals(RouteScoped.class)
                    || scope.equals(NormalRouteScoped.class);
        }

        private boolean isComponent() {
            for (Type type : bean.getTypes()) {
                if (type instanceof Class
                        && Component.class.isAssignableFrom((Class) type)) {
                    return true;
                }
            }
            return false;
        }

        private Optional<RouteScopeOwner> getRouteScopeOwner() {
            for (Annotation ann : bean.getQualifiers()) {
                if (ann instanceof RouteScopeOwner) {
                    return Optional.of((RouteScopeOwner) ann);
                }
            }
            return Optional.empty();
        }

    }

    /**
     * Represents a deployment problem to be passed to the container. Message
     * and stacktrace will appear in server log. It is not thrown, or caught.
     */
    static class DeploymentProblem extends Throwable {

        enum ErrorCode {
            NORMAL_SCOPED_COMPONENT,
            NON_ROUTE_SCOPED_HAVE_OWNER,
            OWNER_IS_NOT_ROUTE_COMPONENT
        }

        private final Type baseType;
        private final ErrorCode errorCode;

        private DeploymentProblem(String message, Type baseType,
                ErrorCode errorCode) {
            super(message);
            this.baseType = baseType;
            this.errorCode = errorCode;
        }

        /**
         * For testing purposes only.
         *
         * @return annotated base type of the invalid bean
         */
        Type getBaseType() {
            return baseType;
        }

        /**
         * For testing purposes only.
         *
         * @return errorCode of the invalid bean
         */
        ErrorCode getErrorCode() {
            return errorCode;
        }
    }

    private interface BeanValidator {

        boolean isInvalid(BeanInfo beanInfo);

        ErrorCode getErrorCode();

        String getErrorMessage(BeanInfo beanInfo);

    }

    private class NormalScopedComponentValidator implements BeanValidator {

        @Override
        public boolean isInvalid(BeanInfo beanInfo) {
            return beanInfo.isComponent()
                    && beanInfo.isNormalScoped(beanManager);
        }

        @Override
        public ErrorCode getErrorCode() {
            return NORMAL_SCOPED_COMPONENT;
        }

        @Override
        public String getErrorMessage(BeanInfo beanInfo) {
            return String.format(
                    "Normal scoped Vaadin components are not supported. "
                            + "'%s' should not belong to a normal scope.",
                    beanInfo.getBaseType().getTypeName());
        }

    }

    private class OwnerIsNotRouteComponentValidator implements BeanValidator {

        @Override
        public boolean isInvalid(BeanInfo beanInfo) {
            return beanInfo.isRouteScoped()
                    && beanInfo.getRouteScopeOwner().map(RouteScopeOwner::value)
                            .filter(DeploymentValidator::isNonRouteComponent)
                            .isPresent();
        }

        @Override
        public ErrorCode getErrorCode() {
            return OWNER_IS_NOT_ROUTE_COMPONENT;
        }

        @Override
        public String getErrorMessage(BeanInfo beanInfo) {
            return String.format(
                    "'@%s' should define a route component on '%s'.",
                    RouteScopeOwner.class.getSimpleName(),
                    beanInfo.getBaseType().getTypeName());
        }

    }

    private class NonRouteScopedHaveOwnerValidator implements BeanValidator {

        @Override
        public boolean isInvalid(BeanInfo beanInfo) {
            return !beanInfo.isRouteScoped()
                    && beanInfo.getRouteScopeOwner().isPresent();
        }

        @Override
        public ErrorCode getErrorCode() {
            return NON_ROUTE_SCOPED_HAVE_OWNER;
        }

        @Override
        public String getErrorMessage(BeanInfo beanInfo) {
            return String.format(
                    "'%s' should be '@%s' or '@%s' to have a '@%s'.",
                    beanInfo.getBaseType().getTypeName(),
                    RouteScoped.class.getSimpleName(),
                    NormalRouteScoped.class.getSimpleName(),
                    RouteScopeOwner.class.getSimpleName());
        }

    }

    @Inject
    private BeanManager beanManager;

    private final List<BeanValidator> validators = Arrays.asList(
            new NormalScopedComponentValidator(),
            new OwnerIsNotRouteComponentValidator(),
            new NonRouteScopedHaveOwnerValidator());

    void validate(Set<BeanInfo> infoSet, Consumer<Throwable> problemConsumer) {
        infoSet.forEach(info -> validateBean(info, problemConsumer));
    }

    private void validateBean(BeanInfo beanInfo,
            Consumer<Throwable> problemConsumer) {
        validators.stream().filter(validator -> validator.isInvalid(beanInfo))
                .map(validator -> new DeploymentProblem(
                        validator.getErrorMessage(beanInfo),
                        beanInfo.getBaseType(), validator.getErrorCode()))
                .forEach(problemConsumer);
    }

    private static boolean isNonRouteComponent(Type type) {
        if (!(type instanceof Class)) {
            return true;
        }
        Class clazz = (Class) type;
        // RouteRegistry contains filtered route targets,
        // but we want to ignore filters here.
        return !clazz.isAnnotationPresent(Route.class)
                && !HasErrorParameter.class.isAssignableFrom(clazz)
                && !RouterLayout.class.isAssignableFrom(clazz);
    }

}
