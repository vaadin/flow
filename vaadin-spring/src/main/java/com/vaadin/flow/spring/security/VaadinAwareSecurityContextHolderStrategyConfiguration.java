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

import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

import com.vaadin.flow.spring.SpringSecurityAutoConfiguration;

/**
 * Provides configuration of Vaadin aware {@link SecurityContextHolderStrategy}
 *
 * @deprecated A {@link SecurityContextHolderStrategy} implementation is now
 *             automatically provided by {@link SpringSecurityAutoConfiguration}
 *             and it is no more necessary to import this configuration class,
 *             which is now no-op. The default strategy implementation is
 *             {@link VaadinAwareSecurityContextHolderStrategy} and can be
 *             overridden by providing a custom bean.
 */
@Deprecated(since = "25.0", forRemoval = true)
@Configuration
public class VaadinAwareSecurityContextHolderStrategyConfiguration {
}
