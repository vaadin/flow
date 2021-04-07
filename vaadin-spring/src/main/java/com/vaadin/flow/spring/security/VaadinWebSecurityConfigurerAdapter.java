package com.vaadin.flow.spring.security;

import com.vaadin.flow.spring.VaadinSpringSecurity;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Provides basic Vaadin security configuration for the project.
 * <p>
 * Sets up security rules for a Vaadin application and restricts all URLs except
 * for public resources and internal Vaadin URLs to authenticated user.
 * <p>
 * The default behavior can be altered by extending the public/protected methods
 * in the class.
 * <p>
 * To use this, create your own web security configurer adapter class by
 * extending this class instead of <code>WebSecurityConfigurerAdapter</code> and
 * annotate it with <code>@EnableWebSecurity</code> and
 * <code>@Configuration</code>.
 * <p>
 * For example <code>
&#64;EnableWebSecurity
&#64;Configuration
public class MySecurityConfigurerAdapter extends VaadinWebSecurityConfigurerAdapter {
    
} 
 * </code>
 * 
 */
public abstract class VaadinWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

    /**
     * The paths listed as "ignoring" in this method are handled without any Spring
     * Security involvement. They have no access to any security context etc.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void configure(WebSecurity web) throws Exception {
        VaadinSpringSecurity.configure(web);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        VaadinSpringSecurity.configure(http);
    }

}
