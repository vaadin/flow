package com.vaadin.flow.plugin.base;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendToolsSettings;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.polymer2lit.FrontendConverter;
import com.vaadin.polymer2lit.ServerConverter;

public class ConvertPolymerExecutor implements AutoCloseable {
    private static final String SERVER_GLOB = "**/*.java";
    private static final String FRONTEND_GLOB = "**/*.js";

    private PluginAdapterBase adapter;

    private String customPath;

    private boolean useLit1;

    private boolean disableOptionalChaining;

    private ServerConverter serverConverter;

    private FrontendConverter frontendConverter;

    public ConvertPolymerExecutor(PluginAdapterBase adapter, String customPath,
            boolean useLit1, boolean disableOptionalChaining)
            throws URISyntaxException, IOException {
        this.adapter = adapter;
        this.customPath = customPath;
        this.useLit1 = useLit1;
        this.disableOptionalChaining = disableOptionalChaining;
        this.serverConverter = new ServerConverter();
        this.frontendConverter = new FrontendConverter(
                new FrontendTools(getFrontendToolsSettings()));
    }

    @Override
    public void close() throws IOException {
        this.frontendConverter.close();
    }

    public void execute() throws IOException, InterruptedException {
        Path lookupPath = getLookupPath();

        for (Path filePath : getFilePathsByGlob(lookupPath, SERVER_GLOB)) {
            adapter.logInfo("Processing " + filePath.toString() + "...");
            serverConverter.convertFile(filePath);
        }

        for (Path filePath : getFilePathsByGlob(lookupPath, FRONTEND_GLOB)) {
            adapter.logInfo("Processing " + filePath.toString() + "...");
            frontendConverter.convertFile(filePath, useLit1,
                    disableOptionalChaining);
        }
    }

    private List<Path> getFilePathsByGlob(Path baseDir, String glob)
            throws IOException {
        PathMatcher matcher = FileSystems.getDefault()
                .getPathMatcher("glob:" + glob);

        try (Stream<Path> walk = Files.walk(baseDir)) {
            return walk.filter(path -> matcher.matches(path))
                    .filter(path -> !path.toString().contains("node_modules"))
                    .collect(Collectors.toList());
        }
    }

    private Path getLookupPath() {
        if (customPath != null) {
            return Paths.get(adapter.projectBaseDirectory().toString(),
                    customPath);
        }

        return adapter.projectBaseDirectory();
    }

    private FrontendToolsSettings getFrontendToolsSettings()
            throws URISyntaxException {
        FrontendToolsSettings settings = new FrontendToolsSettings(
                adapter.npmFolder().getAbsolutePath(),
                () -> FrontendUtils.getVaadinHomeDirectory().getAbsolutePath());
        settings.setNodeDownloadRoot(adapter.nodeDownloadRoot());
        settings.setNodeVersion(adapter.nodeVersion());
        settings.setAutoUpdate(adapter.nodeAutoUpdate());
        settings.setUseGlobalPnpm(adapter.useGlobalPnpm());
        settings.setForceAlternativeNode(adapter.requireHomeNodeExec());
        return settings;
    }
}
