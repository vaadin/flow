package com.vaadin.base.devserver;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vaadin.base.devserver.startup.AbstractDevModeTest;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.server.frontend.FrontendTools;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class AbstractDevServerRunnerTest extends AbstractDevModeTest {

    private class DummyRunner extends AbstractDevServerRunner {

        protected DummyRunner() {
            super(lookup, 0, npmFolder,
                    CompletableFuture.completedFuture(null));
        }

        @Override
        protected File getServerBinary() {
            return new File("dummy.bin");
        }

        @Override
        protected File getServerConfig() {
            return new File("dummy.config");
        }

        @Override
        protected String getServerName() {
            return "Dummy server";
        }

        @Override
        protected List<String> getServerStartupCommand(FrontendTools tools) {
            List<String> commands = new ArrayList<>();
            commands.add("echo");
            return commands;
        }

        @Override
        protected Pattern getServerSuccessPattern() {
            return Pattern.compile("Dummy success");
        }

        @Override
        protected Pattern getServerFailurePattern() {
            return Pattern.compile("Dummy fail");
        }

        @Override
        protected boolean checkConnection() {
            return true;
        }

        @Override
        public HttpURLConnection prepareConnection(String path, String method)
                throws IOException {
            return Mockito.mock(HttpURLConnection.class);
        }

    }

    @Test
    public void shouldPassEncodedUrlToDevServer() throws Exception {
        handler = new DummyRunner();
        DevModeHandler devServer = Mockito.spy(handler);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.when(response.getOutputStream())
                .thenReturn(Mockito.mock(ServletOutputStream.class));
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn("/foo%20bar");
        Mockito.when(request.getPathInfo()).thenReturn("foo bar");
        Mockito.when(request.getHeaderNames())
                .thenReturn(Collections.emptyEnumeration());

        AtomicReference<String> requestedPath = new AtomicReference<>();
        Mockito.when(devServer.prepareConnection(Mockito.any(), Mockito.any()))
                .then(invocation -> {
                    requestedPath.set((String) invocation.getArguments()[0]);
                    return Mockito.mock(HttpURLConnection.class);
                });
        devServer.serveDevModeRequest(request, response);
        Assert.assertEquals("foo%20bar", requestedPath.get());

    }
}
