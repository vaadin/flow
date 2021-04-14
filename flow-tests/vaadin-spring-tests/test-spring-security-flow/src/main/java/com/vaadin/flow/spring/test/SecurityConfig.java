package com.vaadin.flow.spring.test;

import com.vaadin.flow.spring.security.VaadinWebSecurityConfigurerAdapter;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.InMemoryUserDetailsManagerConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity
@Configuration
public class SecurityConfig extends VaadinWebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Admin only access
        http.authorizeRequests().antMatchers("/admin-only/**").hasAnyRole("ADMIN");

        super.configure(http);

        FormLoginConfigurer<HttpSecurity> formLogin = http.formLogin();
        formLogin.loginPage("/login").permitAll();
        http.csrf().ignoringAntMatchers("/login");

        // allow non-interactive logout via /logout and redirect to / after logout
        http.logout().logoutSuccessUrl("/");
        http.logout().logoutRequestMatcher(new AntPathRequestMatcher("/logout"));
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        super.configure(web);
        web.ignoring().antMatchers("/public/**");
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> inmem = auth.inMemoryAuthentication();
        inmem.withUser("user").password("{noop}user").roles("USER");
        inmem.withUser("admin").password("{noop}admin").roles("ADMIN");
    }
}
