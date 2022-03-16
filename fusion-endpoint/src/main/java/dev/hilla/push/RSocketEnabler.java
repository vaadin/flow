package dev.hilla.push;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class RSocketEnabler
        implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        Integer serverPort = environment.getProperty("server.port",
                Integer.class);
        int rsocketPort = serverPort + 1;
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.rsocket.server.port", rsocketPort);
        properties.put("spring.rsocket.server.transport", "websocket");
        environment.getPropertySources()
                .addFirst(new MapPropertySource("Vaadin dynamic", properties));
    }
}
