/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.Version;

import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_JS;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_TS;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generate <code>index.js</code> if it is missing in frontend folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 3.0
 */
public class TaskGenerateIndexTs extends AbstractTaskClientGenerator {

    private final File frontendDirectory;
    private File generatedImports;
    private final File buildDirectory;

    /**
     * Create a task to generate <code>index.js</code> if necessary.
     *
     * @param frontendDirectory
     *            frontend directory is to check if the file already exists
     *            there.
     * @param generatedImports
     *            the flow generated imports file to include in the
     *            <code>index.js</code>
     * @param outputDirectory
     *            the build output directory
     */
    TaskGenerateIndexTs(File frontendDirectory, File generatedImports,
            File buildDirectory) {
        this.frontendDirectory = frontendDirectory;
        this.generatedImports = generatedImports;
        this.buildDirectory = buildDirectory;
    }

    @Override
    protected File getGeneratedFile() {
        return new File(new File(frontendDirectory, FrontendUtils.GENERATED),
                INDEX_TS);
    }

    @Override
    protected boolean shouldGenerate() {
        File indexTs = new File(frontendDirectory, INDEX_TS);
        File indexJs = new File(frontendDirectory, INDEX_JS);
        compareActualIndexTsOrJsWithIndexTempalate(indexTs, indexJs);
        return !indexTs.exists() && !indexJs.exists();
    }

    @Override
    protected String getFileContent() throws IOException {
        String indexTemplate;
        try (InputStream indexTsStream = getClass()
                .getResourceAsStream(INDEX_TS)) {
            indexTemplate = IOUtils.toString(indexTsStream, UTF_8);
        }
        String relativizedImport = ensureValidRelativePath(
                FrontendUtils.getUnixRelativePath(buildDirectory.toPath(),
                        generatedImports.toPath()));

        String generatedDirRelativePathToBuildDir = FrontendUtils
                .getUnixRelativePath(
                        getGeneratedFile().getParentFile().toPath(),
                        buildDirectory.toPath());

        relativizedImport = relativizedImport
                // replace `./` with `../../target/` to make it work
                .replaceFirst("^./", generatedDirRelativePathToBuildDir + "/");

        return indexTemplate.replace("[to-be-generated-by-flow]",
                relativizedImport);
    }

    /**
     * Ensure that the given relative path is valid as an import path. NOTE:
     * expose only for testing purpose.
     *
     * @param relativePath
     *            given relative path
     * @return valid import path
     */
    static String ensureValidRelativePath(String relativePath) {
        if (!relativePath.startsWith(".")) {
            relativePath = "./" + relativePath;
        }
        return relativePath;
    }

    private void compareActualIndexTsOrJsWithIndexTempalate(File indexTs,
            File indexJs) {
        if (indexTs.exists() || indexJs.exists()) {
            File indexFileExist = indexTs.exists() ? indexTs : indexJs;
            String indexContent = null;
            String indexTemplate = null;
            try {
                indexContent = IOUtils.toString(indexFileExist.toURI(), UTF_8);
                indexTemplate = getFileContent();
            } catch (IOException e) {
                log().warn("Failed to read file content", e);
            }
            if (indexContent != null && !indexContent.equals(indexTemplate)) {
                UsageStatistics.markAsUsed(Constants.STATISTIC_ROUTING_CLIENT,
                        Version.getFullVersion());
            }
        }
    }

}
