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
package com.vaadin.spring.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * VaadinApplicationContext allows static access to the {@link org.springframework.context.ApplicationContext}.
 * This implementation exists to provide access from non-managed spring beans.
 * 
 * <p>An VaadinApplicationContext provides:
 * <ul>
 * <li>Access to the Spring {@link org.springframework.context.ApplicationContext}.
 * </ul> 
 * 
 * @author G.J.R. Timmer
 * @see org.springframework.context.ApplicationContext
 * 
 * TODO is this class needed in the base add-on? maybe not if binding a WebApplicationContext to a servlet instead of the whole servlet context
 */
public class VaadinApplicationContext implements InitializingBean, ApplicationContextAware {

	private static Logger logger = LoggerFactory.getLogger(VaadinApplicationContext.class);
	
	private static ApplicationContext context;
	
	/**
     * Return the spring {@link org.springframework.context.ApplicationContext}
     * @return the spring {@link org.springframework.context.ApplicationContext}
     */
    public static ApplicationContext getContext() {
        return context;
    }

	/**
     * @see {@link import org.springframework.context.ApplicationContextAware}
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

	/**
     * @see {@link org.springframework.beans.factory.InitializingBean}
     */
    @Override
    public void afterPropertiesSet() throws Exception {

        logger.debug("{} initialized", getClass().getName());

	}
}
