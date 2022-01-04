package dev.hilla;

import dev.hilla.auth.HillaAccessChecker;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = { ServletContextTestSetup.class,
        HillaEndpointProperties.class })
@ContextConfiguration(classes = HillaControllerConfiguration.class)
@RunWith(SpringRunner.class)
public class HillaControllerConfigurationTest {

    @Autowired
    private EndpointRegistry endpointRegistry;

    @Autowired
    private HillaAccessChecker hillaAccessChecker;

    @Test
    public void dependenciesAvailable() {
        Assert.assertNotNull(endpointRegistry);
        Assert.assertNotNull(hillaAccessChecker);
    }
}
