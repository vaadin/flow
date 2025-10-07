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
package com.vaadin.flow.spring.flowsecurity;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.vaadin.flow.i18n.DefaultI18NProvider;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.auth.DefaultMenuAccessControl;
import com.vaadin.flow.server.auth.MenuAccessControl;
import com.vaadin.flow.spring.i18n.DefaultI18NProviderFactory;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public I18NProvider customI18nProvider() {
        return new I18NProvider() {
            final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
            {
                messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
                messageSource.addBasenames("custom-i18n/translations");
            }

            @Override
            public List<Locale> getProvidedLocales() {
                return List.of(Locale.FRANCE);
            }

            @Override
            public String getTranslation(String key, Locale locale,
                    Object... params) {
                return messageSource.getMessage(key,
                        (params != null ? Stream.of(params).toArray() : null),
                        locale);
            }
        };
    }

    @Bean
    public MenuAccessControl customMenuAccessControl() {
        var menuAccessControl = new DefaultMenuAccessControl();
        menuAccessControl.setPopulateClientSideMenu(
                MenuAccessControl.PopulateClientMenu.ALWAYS);
        return menuAccessControl;
    }
}
