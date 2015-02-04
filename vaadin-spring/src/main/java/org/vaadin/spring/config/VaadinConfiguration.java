/*
 * Copyright 2015 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vaadin.spring.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.vaadin.spring.context.VaadinApplicationContext;
import org.vaadin.spring.internal.VaadinUIScope;
import org.vaadin.spring.navigator.SpringViewProvider;

/**
 * Spring configuration for registering the custom Vaadin scopes,
 * the {@link SpringViewProvider view provider} and some other stuff.
 *
 * @author Josh Long (josh@joshlong.com)
 * @author Petter Holmström (petter@vaadin.com)
 * @author Gert-Jan Timmer (gjr.timmer@gmail.com)
 * @see org.vaadin.spring.annotation.EnableVaadin
 */
@Configuration
public class VaadinConfiguration implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Bean
    static VaadinUIScope vaadinUIScope() {
        return new VaadinUIScope();
    }

    @Bean
    SpringViewProvider viewProvider() {
        return new SpringViewProvider(applicationContext);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    @Bean
    VaadinApplicationContext vaadinApplicationContext() {
    	return new VaadinApplicationContext();
    }
}
