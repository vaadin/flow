package com.vaadin.flow.spring.test;

import javax.servlet.Servlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// This class, as well as the optional spring-boot-starter-jetty dependency,
// can be removed once https://github.com/eclipse/jetty.project/issues/6400
// is fixed and spring-boot is updated to the latest jetty version
// containing the fix or spring-boot changed their configuration.
@Configuration
@ConditionalOnClass({Servlet.class, Server.class, Loader.class,
                     WebAppContext.class})
public class EmbeddedJettyConfiguration {

    @Bean
    JettyServerCustomizer stopTimeoutCustomizer() {
        return (server) -> server.setStopTimeout(1);
    }
}
