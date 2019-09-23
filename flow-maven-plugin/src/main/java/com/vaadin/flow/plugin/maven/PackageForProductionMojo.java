/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;

import com.vaadin.flow.plugin.common.AnnotationValuesExtractor;
import com.vaadin.flow.plugin.common.FrontendDataProvider;
import com.vaadin.flow.plugin.common.FrontendToolsManager;
import com.vaadin.flow.plugin.common.RunnerManager;
import com.vaadin.flow.plugin.production.TranspilationStep;

import static com.vaadin.flow.plugin.common.FlowPluginFrontendUtils.getClassFinder;

/**
 * Goal that prepares all web files from
 * {@link PackageForProductionMojo#transpileEs6SourceDirectory} for production
 * mode: minifies, transpiles and bundles them.
 *
 * @since 1.0
 */
@Mojo(name = "package-for-production", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class PackageForProductionMojo extends FlowModeAbstractMojo {
    /**
     * Directory where the source files to use for transpilation are located.
     * <b>Note!</b> This should match <code>copyOutputDirectory</code>
     * configuration when using the `copy-production-files` goal.
     */
    @Parameter(name = "transpileEs6SourceDirectory", defaultValue = "${project.build.directory}/frontend/", required = true)
    private File transpileEs6SourceDirectory;

    /**
     * The directory where we process the files from. The default is
     * <code>${project.build}</code>.
     */
    @Parameter(name = "transpileWorkingDirectory", defaultValue = "${project.build.directory}/", required = true)
    private File transpileWorkingDirectory;

    /**
     * Target base directory where the transpilation output should be stored.
     * The default is
     * <code>${project.build.directory}/${project.build.finalName}</code>.
     */
    @Parameter(name = "transpileOutputDirectory")
    private File transpileOutputDirectory;

    /**
     * Name of the ES6 directory.The default is <code>frontend-es6</code>.
     */
    @Parameter(name = "es6OutputDirectoryName", defaultValue = "frontend-es6", required = true)
    private String es6OutputDirectoryName;

    /**
     * Name of the ES5 directory.The default is <code>frontend-es5</code>.
     */
    @Parameter(name = "es5OutputDirectoryName", defaultValue = "frontend-es5", required = true)
    private String es5OutputDirectoryName;

    /**
     * If true skip the transpilation of javascript from ES6 to ES5.
     */
    @Parameter(property = "skipEs5", defaultValue = "false", required = true)
    private boolean skipEs5;

    /**
     * List of bundle fragments.
     */
    @Parameter
    private List<Fragment> fragments;

    /**
     * If <code>false</code> sources will not be bundled.
     */
    @Parameter(property = "bundle", defaultValue = "true", required = true)
    private boolean bundle;

    /**
     * If <code>false</code> the ES5 and ES6 code will not be minified. This
     * will help in debugging if there are JS exception in runtime.
     */
    @Parameter(property = "minify", defaultValue = "true", required = true)
    private boolean minify;

    /**
     * If <code>false</code> then the bundle will not receive a hash for the
     * content. This will make the bundle not update on content change after it
     * is cached in the browser.
     */
    @Parameter(property = "hash", defaultValue = "true", required = true)
    private boolean hash;

    /**
     * Set the bundle configuration json file.
     */
    @Parameter(property = "bundleConfiguration", defaultValue = "${project.basedir}/bundle-configuration.json")
    private File bundleConfiguration;

    /**
     * Set the web components module files output folder name. The default is
     * <code>vaadin-web-components</code>. The folder is used to generate the
     * web component module files.
     */
    @Parameter(property = "webComponentOutputDirectoryName", defaultValue = "vaadin-web-components")
    private String webComponentOutputDirectoryName;

    /**
     * Defines the path to node executable to use. If specified,
     * {@code nodeVersion} parameter is ignored.
     */
    @Parameter(name = "nodePath")
    private File nodePath;

    /**
     * Defines the node version to download and use, if {@code nodePath} is not
     * set. The default is <code>v10.16.0</code>.
     */
    @Parameter(name = "nodeVersion", defaultValue = "v10.16.0")
    private String nodeVersion;

    /**
     * Defines the path to yarn executable to use. If specified,
     * {@code yarnVersion} parameter is ignored.
     */
    @Parameter(name = "yarnPath")
    private File yarnPath;

    /**
     * Defines the yarn version to download and use, if {@code yarnVersion} is
     * not set. The default is <code>v1.6.0</code>.
     */
    @Parameter(name = "yarnVersion", defaultValue = "v1.6.0")
    private String yarnVersion;

    /**
     * Set yarn network concurrency.The default is <code>-1</code>.
     */
    @Parameter(name = "yarnNetworkConcurrency", defaultValue = "-1")
    private int yarnNetworkConcurrency;

    /**
     * Defines the URL of npm modules. Yarn will use the given NPM registry URL
     * if valid, otherwise the default registry will be used.
     */
    @Parameter(property = "npmRegistryURL")
    private String npmRegistryURL;

    /**
     * If {@code true}, attempts to detect frontend tools (Node, Yarn) in the
     * system and use them for processing the frontend files.
     */
    @Parameter(property = "autodetectTools", defaultValue = "false")
    private boolean autodetectTools;

    /**
     * If <code>false</code> then maven proxies will be used in the
     * ProxyConfiguration.
     */
    @Parameter(property = "ignoreMavenProxies", defaultValue = "true", required = true)
    private boolean ignoreMavenProxies;

    /**
     * Set the maven session which will be used for maven proxies if they are
     * not ignored.
     */
    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    /**
     * The maven settings decrypter.
     */
    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();

        // Do nothing when not in compatibility mode
        if (!compatibility) {
            getLog().info(
                    "Skipped `package-for-production` goal because compatibility mode is not set.");
            return;
        }

        if (transpileOutputDirectory == null) {
            if ("jar".equals(project.getPackaging()) && project.getArtifactMap()
                    .containsKey("com.vaadin:vaadin-spring-boot-starter")) {
                // in spring boot project there is not web app directory
                transpileOutputDirectory = new File(
                        project.getBuild().getOutputDirectory(),
                        "META-INF/resources");
            } else {
                // the default assumes basic war project
                transpileOutputDirectory = new File(
                        project.getBuild().getDirectory(),
                        project.getBuild().getFinalName());
            }
        }

        FrontendDataProvider frontendDataProvider = new FrontendDataProvider(
                bundle, minify, hash, transpileEs6SourceDirectory,
                new AnnotationValuesExtractor(getClassFinder(project)),
                bundleConfiguration, webComponentOutputDirectoryName,
                getFragmentsData(fragments));

        FrontendToolsManager frontendToolsManager = new FrontendToolsManager(
                transpileWorkingDirectory, es5OutputDirectoryName,
                es6OutputDirectoryName, frontendDataProvider,
                getRunnerManager());

        new TranspilationStep(frontendToolsManager, yarnNetworkConcurrency)
                .transpileFiles(transpileEs6SourceDirectory,
                        transpileOutputDirectory, skipEs5);
    }

    private RunnerManager getRunnerManager() {
        return new RunnerManager.Builder(transpileWorkingDirectory,
                getProxyConfig()).versionsToDownload(nodeVersion, yarnVersion)
                        .localInstallations(nodePath, yarnPath)
                        .autodetectTools(autodetectTools)
                        .npmRegistryUrl(npmRegistryURL).build();
    }

    private Map<String, Set<String>> getFragmentsData(
            List<Fragment> mavenFragments) {
        return Optional.ofNullable(mavenFragments)
                .orElse(Collections.emptyList()).stream()
                .peek(this::verifyFragment).collect(Collectors
                        .toMap(Fragment::getName, Fragment::getFiles));
    }

    private void verifyFragment(Fragment fragment) {
        if (fragment.getName() == null || fragment.getFiles() == null
                || fragment.getFiles().isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Each fragment definition should have a name and list of files to include defined. Got incorrect definition: '%s'",
                    fragment));
        }
    }

    private ProxyConfig getProxyConfig() {
        if (ignoreMavenProxies) {
            return new ProxyConfig(Collections.emptyList());
        }
        return new ProxyConfig(getMavenProxies().stream()
                .filter(Proxy::isActive)
                .map(proxy -> decrypter
                        .decrypt(new DefaultSettingsDecryptionRequest(proxy)))
                .map(SettingsDecryptionResult::getProxy).map(this::createProxy)
                .collect(Collectors.toList()));
    }

    private List<Proxy> getMavenProxies() {
        if (session == null || session.getSettings() == null
                || session.getSettings().getProxies() == null
                || session.getSettings().getProxies().isEmpty()) {
            return Collections.emptyList();
        }
        return session.getSettings().getProxies();
    }

    private ProxyConfig.Proxy createProxy(Proxy proxy) {
        return new ProxyConfig.Proxy(proxy.getId(), proxy.getProtocol(),
                proxy.getHost(), proxy.getPort(), proxy.getUsername(),
                proxy.getPassword(), proxy.getNonProxyHosts());
    }

    @Override
    boolean isDefaultCompatibility() {
        return true;
    }
}
