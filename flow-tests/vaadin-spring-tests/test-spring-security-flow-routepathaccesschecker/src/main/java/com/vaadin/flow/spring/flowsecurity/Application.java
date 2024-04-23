package com.vaadin.flow.spring.flowsecurity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.vaadin.flow.server.auth.DefaultMenuAccessControl;
import com.vaadin.flow.server.auth.MenuAccessControl;

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
    public MenuAccessControl customMenuAccessControl() {
        var menuAccessControl = new DefaultMenuAccessControl();
        menuAccessControl.setPopulateClientSideMenu(
                MenuAccessControl.PopulateClientMenu.ALWAYS);
        return menuAccessControl;
    }
}
