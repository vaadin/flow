package com.vaadin.flow.plugin.base;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.polymer2lit.FrontendConverter;
import com.vaadin.polymer2lit.ServerConverter;

import org.codehaus.plexus.util.ReflectionUtils;

public class ConvertPolymerExecutorTest {
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Mock
    private MockedConstruction<FrontendConverter> frontendConverterMock;

    @Mock
    private MockedConstruction<ServerConverter> serverConverterMock;

    @Mock
    private PluginAdapterBase adapter;

    @Mock
    private ConvertPolymerExecutor executor;

    private AutoCloseable closeable;

    @Before
    public void init()
            throws IOException, URISyntaxException, IllegalAccessException {
        closeable = MockitoAnnotations.openMocks(this);
        TestUtil.stubPluginAdapterBase(adapter, tmpDir.getRoot());
    }

    @After
    public void teardown() throws Exception {
        closeable.close();
    }

    @Test
    public void convertFrontend()
            throws URISyntaxException, IOException, InterruptedException {
        try (ConvertPolymerExecutor executor = new ConvertPolymerExecutor(
                adapter, "**/*.java", "**/*.js", false, false)) {
            executor.execute();

            Mockito.verify(frontendConverterMock.constructed().get(0))
                    .convertFile(Path.of("/file1.js"), false, false);
        }

    }

    // @Test
    // public void convertServer() {

    // }
}
