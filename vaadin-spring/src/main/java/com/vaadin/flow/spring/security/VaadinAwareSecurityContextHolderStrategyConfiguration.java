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
package com.vaadin.flow.spring.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

/**
 * Provides configuration of Vaadin aware {@link SecurityContextHolderStrategy}
 */
@Configuration
public class VaadinAwareSecurityContextHolderStrategyConfiguration {

    /**
     * Registers {@link SecurityContextHolderStrategy} bean.
     * <p>
     * Beans of this type will automatically be used by
     * {@link org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration}
     * to configure the current {@link SecurityContextHolderStrategy}.
     *
     * @return the Vaadin aware security context holder strategy
     */
    @Bean(name = "VaadinSecurityContextHolderStrategy")
    @ConditionalOnMissingBean
    public VaadinAwareSecurityContextHolderStrategy securityContextHolderStrategy() {
        VaadinAwareSecurityContextHolderStrategy vaadinAwareSecurityContextHolderStrategy = new VaadinAwareSecurityContextHolderStrategy();
        // Use a security context holder that can find the context from Vaadin
        // specific classes
        SecurityContextHolder.setContextHolderStrategy(
                vaadinAwareSecurityContextHolderStrategy);
        return vaadinAwareSecurityContextHolderStrategy;
    }

}
