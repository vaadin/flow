package com.vaadin.flow.plugin.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class DefaultReflectorProvider implements ReflectorProvider {
    protected static final Set<String> MAVEN_CLASSLOADER_RESERVED_GROUP_IDS = Set.of(
            "org.apache.maven",
            "org.codehaus.plexus",
            "org.slf4j",
            "org.eclipse.sisu"
    );

    private static final Set<String> MANDATORY_PLUGIN_DEPENDENCIES = Set.of(
            "org.reflections:reflections:jar",
            "org.zeroturnaround:zt-exec:jar"
    );

    protected static final Set<String> DEFAULT_PROJECT_ARTIFACT_SCOPES_INCLUSION = Set.of(
            Artifact.SCOPE_COMPILE,
            Artifact.SCOPE_RUNTIME,
            Artifact.SCOPE_SYSTEM,
            Artifact.SCOPE_PROVIDED
    );

    protected final Set<FastReflectorConfig.ArtifactSelector> fastDefaultExcludes = new HashSet<>(Set.of(
            new FastReflectorConfig.ArtifactSelector("com.vaadin.external*")
    ));

    protected final Set<FastReflectorConfig.ArtifactSelector> fastDefaultIncludes = new HashSet<>(Set.of(
            new FastReflectorConfig.ArtifactSelector("*vaadin*"),
            new FastReflectorConfig.ArtifactSelector(null, "*vaadin*")
    ));

    protected final FastReflectorConfig fastReflectorConfig;
    protected final Log log;

    public DefaultReflectorProvider(final FastReflectorConfig fastReflectorConfig, final Log log) {
        this.fastReflectorConfig =
                Objects.requireNonNullElseGet(fastReflectorConfig, FastReflectorConfig::new);
        this.log = log;
    }

    @Override
    public String getReflectorClassIdentifier() {
        return DefaultReflector.class.getName();
    }

    @Override
    public Reflector adaptFrom(final Object reflector) {
        if (reflector instanceof final DefaultReflector onSameClassLoader) {
            return onSameClassLoader;
        } else if (DefaultReflector.class.getName().equals(reflector.getClass().getName())) {
            return new DefaultReflector(reflector);
        }

        throw new IllegalArgumentException(
                "Object of type " + reflector.getClass().getName() + " is not a compatible Reflector");
    }

    @Override
    public Reflector createNew(final MavenProject project, final MojoExecution mojoExecution) {
        return new DefaultReflector(createIsolatedClassLoader(project, mojoExecution));
    }

    protected ReflectorIsolatedClassLoader createIsolatedClassLoader(
            final MavenProject project,
            final MojoExecution mojoExecution) {
        final List<URLWrapper> urlInfo = Stream.concat(
                        getOutputDirectoryLocation(project),
                        getArtifactLocations(project, mojoExecution)
                )
                .toList();

        if (log.isDebugEnabled()) {
            log.debug("Isolated classloader will use:"
                    + System.lineSeparator()
                    + urlInfo.stream()
                    .map(w -> " - " + w.toString())
                    .sorted()
                    .collect(Collectors.joining(System.lineSeparator())));
        }

        return new CombinedClassLoader(
                urlInfo.stream()
                        .map(URLWrapper::url)
                        .toArray(URL[]::new),
                getMavenApiClassLoader(mojoExecution),
                urlInfo.stream()
                        .filter(URLWrapper::scan)
                        .map(URLWrapper::url)
                        .toArray(URL[]::new));
    }

    protected Stream<URLWrapper> getOutputDirectoryLocation(final MavenProject project) {
        return Optional.ofNullable(project.getBuild().getOutputDirectory())
                .map(File::new)
                .stream()
                .map(this::convertToUrl)
                .map(url -> new URLWrapper(url, true));
    }

    protected Stream<URLWrapper> getArtifactLocations(final MavenProject project, final MojoExecution mojoExecution) {
        final Function<Artifact, String> keyMapper =
                artifact -> artifact.getGroupId() + ":" + artifact.getArtifactId()
                        + (artifact.getClassifier() != null
                        ? ":" + artifact.getClassifier()
                        : "");

        final Map<String, ArtifactWrapper> projectDependencies = new HashMap<>(project
                .getArtifacts().stream()
                .filter(this::shouldIncludeArtifact)
                .map(this::shouldIncludeProjectArtifact)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(a -> keyMapper.apply(a.artifact()), Function.identity())));

        if (mojoExecution != null) {
            final List<Artifact> pluginDependencies = mojoExecution.getMojoDescriptor().getPluginDescriptor()
                    .getArtifacts().stream()
                    .filter(this::shouldIncludeArtifact)
                    .toList();

            // Exclude project artifact that are also defined as mandatory
            // plugin dependencies. The version provided by the plugin will be
            // used to prevent failures during maven build.
            MANDATORY_PLUGIN_DEPENDENCIES.stream()
                    .map(projectDependencies::remove)
                    .filter(a -> log.isDebugEnabled())
                    .filter(Objects::nonNull)
                    .map(ArtifactWrapper::artifact)
                    .forEach(a ->
                            log.debug("Using plugin version of " + a.getGroupId() + ":" + a.getArtifactId()
                                    + " instead of project version"));

            // Preserve required plugin dependency that are not provided by Flow
            // -1: dependency defined on both plugin and project, with different version
            // 0: dependency defined on both plugin and project, with same version
            // 1: dependency defined by the plugin only
            final Map<Integer, List<Artifact>> potentialDuplicates = pluginDependencies
                    .stream().collect(Collectors.groupingBy(pluginArtifact -> {
                        final ArtifactWrapper projectWrapper = projectDependencies.get(keyMapper.apply(pluginArtifact));
                        if (projectWrapper == null) {
                            return 1;
                        } else if (projectWrapper.artifact().getId().equals(pluginArtifact.getId())) {
                            return 0;
                        }
                        return -1;
                    }));

            // Report potential plugin and project dependency versions incompatibilities.
            if (potentialDuplicates.containsKey(-1)) {
                log.warn("""
                        Found dependencies defined with different versions in project and maven plugin.
                        Project dependencies are used, but plugin execution could fail if the versions are incompatible.
                        In case of build failure please analyze the project dependencies and update versions or \
                        configure exclusions for potential offending transitive dependencies.
                        Affected dependencies:
                        """
                        + potentialDuplicates.get(-1)
                        .stream()
                        .map(pluginArtifact -> {
                            final String key = keyMapper.apply(pluginArtifact);
                            return String.format(
                                    "%s: project version [%s], plugin version [%s]",
                                    key,
                                    projectDependencies.get(key).artifact().getBaseVersion(),
                                    pluginArtifact.getBaseVersion());
                        })
                        .collect(Collectors.joining(System.lineSeparator())));
            }

            // Add dependencies defined only by the plugin
            if (potentialDuplicates.containsKey(1)) {
                potentialDuplicates.get(1)
                        .forEach(artifact -> projectDependencies.put(
                                keyMapper.apply(artifact),
                                // Plugin-only artifacts require no scanning
                                new ArtifactWrapper(artifact, false)
                        ));
            }
        }

        return projectDependencies.values().stream()
                .map(w -> new URLWrapper(convertToUrl(w.artifact().getFile()), w.scan()));
    }

    protected boolean shouldIncludeArtifact(final Artifact artifact) {
        // Exclude all maven artifacts to prevent class loading
        // clash with maven.api class realm
        return !MAVEN_CLASSLOADER_RESERVED_GROUP_IDS.contains(artifact.getGroupId());
    }

    protected ArtifactWrapper shouldIncludeProjectArtifact(final Artifact artifact) {
        if (!(artifact.getFile() != null
                && artifact.getArtifactHandler().isAddedToClasspath()
                && DEFAULT_PROJECT_ARTIFACT_SCOPES_INCLUSION.contains(artifact.getScope()))) {
            logArtifactInclusionOrExclusion(artifact, false, "Vaadin default filter");
            return null;
        }

        if (!fastReflectorConfig.isEnabled()) {
            logArtifactInclusionOrExclusion(artifact, true, null);
            return new ArtifactWrapper(artifact, true);
        }

        final FastReflectorConfig.Isolation isolation = fastReflectorConfig.getIsolation();

        // Fast code starts here
        final Optional<FastReflectorConfig.ArtifactSelector> excludeSelector = checkIfArtifactSelectorsMatches(
                isolation.getExcludes(),
                fastDefaultExcludes,
                artifact);
        if (excludeSelector.isPresent()) {
            final FastReflectorConfig.ArtifactSelector selector = excludeSelector.get();
            logArtifactInclusionOrExclusion(artifact, false, "in excludes [" + selector + "]");
            return null;
        }

        final Optional<FastReflectorConfig.ArtifactSelector> includeSelector = checkIfArtifactSelectorsMatches(
                isolation.getIncludes(),
                fastDefaultIncludes,
                artifact);
        if (includeSelector.isPresent()) {
            final FastReflectorConfig.ArtifactSelector selector = includeSelector.get();
            logArtifactInclusionOrExclusion(artifact, true, "in includes [" + selector + "]");
            return new ArtifactWrapper(artifact, selector.isScan());
        }

        // If a jar is inside output/target directory, it's likely a sibling project
        if (isolation.isIncludeWhenInOutputDirectory()
                && isolation.getOutputDirectoryNames().contains(artifact.getFile().getParentFile().getName())) {
            logArtifactInclusionOrExclusion(artifact, true, "source=output/target directory");
            return new ArtifactWrapper(artifact, true);
        }

        logArtifactInclusionOrExclusion(artifact, false, null);
        return null;
    }

    protected void logArtifactInclusionOrExclusion(final Artifact artifact, final boolean include, final String reason) {
        if (log.isDebugEnabled()) {
            log.debug(
                    (include ? "In" : "Ex") + "cluding project artifact "
                            + artifact.getGroupId() + ":" + artifact.getArtifactId()
                            + " from isolated classloader"
                            + (reason != null ? " due to '" + reason + "'" : ""));
        }
    }

    protected Optional<FastReflectorConfig.ArtifactSelector> checkIfArtifactSelectorsMatches(
            final FastReflectorConfig.ArtifactSelectors selectors,
            final Set<FastReflectorConfig.ArtifactSelector> defaults,
            final Artifact artifact) {
        return Stream.concat(
                        selectors.isDefaults() ? defaults.stream() : Stream.empty(),
                        selectors.getAdditional().stream())
                .filter(sel -> checkIfArtifactSelectorMatches(sel, artifact))
                .findFirst();
    }

    protected boolean checkIfArtifactSelectorMatches(
            final FastReflectorConfig.ArtifactSelector selector,
            final Artifact artifact) {
        if (selector.getGroupId() != null && !compareSelector(selector.getGroupId(), artifact.getGroupId())) {
            return false;
        }
        return selector.getArtifactId() == null
                || compareSelector(selector.getArtifactId(), artifact.getArtifactId());
    }

    protected boolean compareSelector(final String selector, final String target) {
        if (selector == null || target == null) {
            return false;
        }

        if (selector.endsWith("*") && selector.startsWith("*")) {
            return target.contains(selector.substring(1, selector.length() - 1));
        } else if (selector.endsWith("*")) {
            return target.startsWith(selector.substring(0, selector.length() - 1));
        } else if (selector.startsWith("*")) {
            return target.endsWith(selector.substring(1));
        } else {
            return selector.equals(target);
        }
    }

    protected ClassLoader getMavenApiClassLoader(final MojoExecution mojoExecution) {
        if (mojoExecution != null) {
            final ClassRealm pluginClassRealm = mojoExecution.getMojoDescriptor()
                    .getPluginDescriptor().getClassRealm();
            try {
                return getMavenAPIFromClassRealm(pluginClassRealm);
            } catch (final NoSuchRealmException e) {
                throw new IllegalStateException(e);
            }
        }

        final ClassLoader mavenApiClassLoader = Mojo.class.getClassLoader();
        if (mavenApiClassLoader instanceof final ClassRealm classRealm) {
            try {
                return getMavenAPIFromClassRealm(classRealm);
            } catch (final NoSuchRealmException e) {
                // Should never happen. In case, ignore the error and use
                // class loader from the Maven class
            }
        }
        return mavenApiClassLoader;
    }

    protected ClassLoader getMavenAPIFromClassRealm(final ClassRealm classRealm) throws NoSuchRealmException {
        return classRealm.getWorld().getRealm("maven.api");
    }

    protected URL convertToUrl(final File file) {
        try {
            return file.toURI().toURL();
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(String.format("Failed to convert file '%s' to URL", file), e);
        }
    }

    public record ArtifactWrapper(
            Artifact artifact,
            boolean scan
    ) {

    }

    public record URLWrapper(
            URL url,
            boolean scan
    ) {
        @Override
        public String toString() {
            return url().toString() + (!scan() ? " NO_SCAN" : "");
        }
    }

    public static class CombinedClassLoader extends ReflectorIsolatedClassLoader {
        protected final ClassLoader delegate;
        protected final URL[] urlsToScan;

        public CombinedClassLoader(final URL[] urls, final ClassLoader delegate, final URL[] urlsToScan) {
            super(urls, null);
            this.delegate = delegate;
            this.urlsToScan = urlsToScan;
        }

        @Override
        public Class<?> loadClass(final String name) throws ClassNotFoundException {
            try {
                return super.loadClass(name);
            } catch (final ClassNotFoundException e) {
                // ignore and continue with delegate class loader
            }
            if (delegate != null) {
                try {
                    return delegate.loadClass(name);
                } catch (final ClassNotFoundException e) {
                    // ignore and continue with platform class loader
                }
            }
            return ClassLoader.getPlatformClassLoader().loadClass(name);
        }

        @Override
        public URL getResource(final String name) {
            URL url = super.getResource(name);
            if (url == null && delegate != null) {
                url = delegate.getResource(name);
            }
            if (url == null) {
                url = ClassLoader.getPlatformClassLoader().getResource(name);
            }
            return url;
        }

        @Override
        public Enumeration<URL> getResources(final String name) throws IOException {
            Enumeration<URL> resources = super.getResources(name);
            if (!resources.hasMoreElements() && delegate != null) {
                resources = delegate.getResources(name);
            }
            if (!resources.hasMoreElements()) {
                resources = ClassLoader.getPlatformClassLoader()
                        .getResources(name);
            }
            return resources;
        }

        @Override
        public URL[] urlsToScan() {
            return urlsToScan;
        }
    }
}
