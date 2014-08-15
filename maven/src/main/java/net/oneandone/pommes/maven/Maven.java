/**
 * Copyright 1&1 Internet AG, https://github.com/1and1/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.oneandone.pommes.maven;

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Separator;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.MetadataBridge;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.repository.legacy.LegacyRepositorySystem;
import org.apache.maven.settings.DefaultMavenSettingsBuilder;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.resolution.VersionRequest;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.eclipse.aether.resolution.VersionResult;
import org.eclipse.aether.transfer.TransferListener;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.eclipse.aether.version.Version;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Maven {
    public static Maven withSettings(World world) throws IOException {
        return withSettings(world, null, null, null);
    }

    /**
     * @param localRepository null to use default
     * @param globalSettings null to use default
     * @param userSettings null to use default
     */
    public static Maven withSettings(World world, FileNode localRepository, FileNode globalSettings, FileNode userSettings)
            throws IOException {
        return withSettings(world, localRepository, globalSettings, userSettings, container(), null, null);
    }

    public static Maven withSettings(World world, FileNode localRepository, FileNode globalSettings, FileNode userSettings,
                                     DefaultPlexusContainer container,
                                     TransferListener transferListener, RepositoryListener repositoryListener) throws IOException {
        RepositorySystem system;
        DefaultRepositorySystemSession session;
        Settings settings;
        LegacyRepositorySystem legacySystem;
        List<ArtifactRepository> repositoriesLegacy;

        try {
            try {
                settings = loadSettings(world, globalSettings, userSettings, container);
            } catch (XmlPullParserException e) {
                throw new IOException("cannot load settings: " + e.getMessage(), e);
            }
            system = container.lookup(RepositorySystem.class);
            session = createSession(transferListener, repositoryListener, system,
                    createLocalRepository(world, localRepository, settings), settings);
            legacySystem = (LegacyRepositorySystem) container.lookup(org.apache.maven.repository.RepositorySystem.class, "default");
            repositoriesLegacy = repositoriesLegacy(legacySystem, settings);
            legacySystem.injectAuthentication(session, repositoriesLegacy);
            legacySystem.injectMirror(session, repositoriesLegacy);
            legacySystem.injectProxy(session, repositoriesLegacy);
            return new Maven(world, system, session, container.lookup(ProjectBuilder.class),
                    RepositoryUtils.toRepos(repositoriesLegacy), repositoriesLegacy);
        } catch (InvalidRepositoryException | ComponentLookupException e) {
            throw new IllegalStateException(e);
        }
    }

    private static LocalRepository createLocalRepository(World world, FileNode localRepository, Settings settings) {
        String localRepositoryStr;
        LocalRepository localRepositoryObj;
        if (localRepository == null) {
            // TODO: who has precedence: repodir from settings or from MAVEN_OPTS
            localRepositoryStr = settings.getLocalRepository();
            if (localRepositoryStr == null) {
                localRepositoryStr = localRepositoryPathFromMavenOpts();
                if (localRepositoryStr == null) {
                    localRepositoryStr = defaultLocalRepositoryDir(world).toPath().toFile().getAbsolutePath();
                }
            }
        } else {
            localRepositoryStr = localRepository.getAbsolute();
        }
        localRepositoryObj = new LocalRepository(localRepositoryStr);
        return localRepositoryObj;
    }

    private static DefaultRepositorySystemSession createSession(TransferListener transferListener, RepositoryListener repositoryListener,
                                                              RepositorySystem system, LocalRepository localRepository, Settings settings) {
        DefaultRepositorySystemSession session;
        final List<Server> servers;

        servers = settings.getServers();
        session = MavenRepositorySystemUtils.newSession();
        session.setAuthenticationSelector(new AuthenticationSelector() {
            @Override
            public Authentication getAuthentication(RemoteRepository repository) {
                Server server;

                server = lookup(repository.getId());
                if (server != null) {
                    if (server.getPassphrase() != null) {
                        throw new UnsupportedOperationException();
                    }
                    if (server.getPrivateKey() != null) {
                        throw new UnsupportedOperationException();
                    }
                    if (server.getUsername() != null) {
                        if (server.getPassword() == null) {
                            throw new IllegalStateException("missing password");
                        }
                        return new AuthenticationBuilder().addUsername(server.getUsername()).addPassword(server.getPassword()).build();
                    }
                }
                return null;
            }

            private Server lookup(String id) {
                for (Server server : servers) {
                    if (id.equals(server.getId())) {
                        return server;
                    }
                }
                return null;
            }


        });
        if (repositoryListener != null) {
            session.setRepositoryListener(repositoryListener);
        }
        if (transferListener != null) {
            session.setTransferListener(transferListener);
        }
        session.setOffline(settings.isOffline());
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepository));
        DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
        for (Mirror mirror : settings.getMirrors()) {
            mirrorSelector.add(mirror.getId(), mirror.getUrl(), mirror.getLayout(), false, mirror.getMirrorOf(),
                               mirror.getMirrorOfLayouts());
        }
        session.setMirrorSelector(mirrorSelector);
        session.setProxySelector(getProxySelector(settings));
        return session;
    }

    private static ProxySelector getProxySelector(Settings settings) {
        DefaultProxySelector selector;
        AuthenticationBuilder builder;

        selector = new DefaultProxySelector();
        for (org.apache.maven.settings.Proxy proxy : settings.getProxies()) {
            builder = new AuthenticationBuilder();
            builder.addUsername(proxy.getUsername()).addPassword(proxy.getPassword());
            selector.add(new org.eclipse.aether.repository.Proxy(proxy.getProtocol(), proxy.getHost(),
                    proxy.getPort(), builder.build()),
                    proxy.getNonProxyHosts());
        }

        return selector;
    }

    //--

    public static FileNode defaultLocalRepositoryDir(World world) {
        return world.file(new File(System.getProperty("user.home"))).join(".m2/repository");
    }

    public static DefaultPlexusContainer container() {
        return container(null, null, Logger.LEVEL_DISABLED);
    }

    public static DefaultPlexusContainer container(ClassWorld classWorld, ClassRealm realm, int loglevel) {
        DefaultContainerConfiguration config;
        DefaultPlexusContainer container;

        config = new DefaultContainerConfiguration();
        if (classWorld != null) {
            config.setClassWorld(classWorld);
        }
        if (realm != null) {
            config.setRealm(realm);
        }
        config.setAutoWiring(true);
        try {
            container = new DefaultPlexusContainer(config);
        } catch (PlexusContainerException e) {
            throw new IllegalStateException(e);
        }
        container.getLoggerManager().setThreshold(loglevel);
        return container;
    }

    //--

    private final World world;
    private final RepositorySystem repositorySystem;
    private final DefaultRepositorySystemSession repositorySession;
    private final List<RemoteRepository> remote;
    private final List<ArtifactRepository> remoteLegacy; // needed to load poms :(

    // TODO: use a project builder that works without legacy classes, esp. without ArtifactRepository ...
    // As far as I know, there's no such project builder as of mvn 3.0.2.
    private final ProjectBuilder builder;

    public Maven(World world, RepositorySystem repositorySystem, DefaultRepositorySystemSession repositorySession, ProjectBuilder builder,
                 List<RemoteRepository> remote, List<ArtifactRepository> remoteLegacy) {
        this.world = world;
        this.repositorySystem = repositorySystem;
        this.repositorySession = repositorySession;
        this.builder = builder;
        this.remote = remote;
        this.remoteLegacy = remoteLegacy;
    }

    public World getWorld() {
        return world;
    }

    public RepositorySystem getRepositorySystem() {
        return repositorySystem;
    }

    public DefaultRepositorySystemSession getRepositorySession() {
        return repositorySession;
    }

    public List<ArtifactRepository> remoteRepositoriesLegacy() {
        return remoteLegacy;
    }

    public List<RemoteRepository> remoteRepositories() {
        return remote;
    }

    public FileNode getLocalRepositoryDir() {
        return world.file(repositorySession.getLocalRepository().getBasedir());
    }

    public FileNode getLocalRepositoryFile(Artifact artifact) {
        return getLocalRepositoryDir().join(repositorySession.getLocalRepositoryManager().getPathForLocalArtifact(artifact));
    }

    public List<FileNode> files(List<Artifact> artifacts) {
        List<FileNode> result;

        result = new ArrayList<FileNode>();
        for (Artifact a : artifacts) {
            result.add(file(a));
        }
        return result;
    }

    public FileNode file(Artifact artifact) {
        return world.file(artifact.getFile());
    }

    //-- resolve

    public FileNode resolve(String groupId, String artifactId, String version) throws ArtifactResolutionException {
        return resolve(groupId, artifactId, "jar", version);
    }

    public FileNode resolve(String groupId, String artifactId, String extension, String version) throws ArtifactResolutionException {
        return resolve(new DefaultArtifact(groupId, artifactId, extension, version));
    }

    public FileNode resolve(String gav) throws ArtifactResolutionException {
        return resolve(new DefaultArtifact(gav));
    }

    public FileNode resolve(Artifact artifact) throws ArtifactResolutionException {
        return resolve(artifact, remote);
    }

    public FileNode resolve(Artifact artifact, List<RemoteRepository> remoteRepositories) throws ArtifactResolutionException {
        ArtifactRequest request;
        ArtifactResult result;

        request = new ArtifactRequest(artifact, remoteRepositories, null);
        result = repositorySystem.resolveArtifact(repositorySession, request);
        if (!result.isResolved()) {
            throw new ArtifactResolutionException(new ArrayList<ArtifactResult>()); // TODO
        }
        return world.file(result.getArtifact().getFile());
    }

    //-- load poms

    public MavenProject loadPom(Artifact artifact) throws RepositoryException, ProjectBuildingException {
        return loadPom(resolve(new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), "pom", artifact.getVersion())), false);
    }

    public MavenProject loadPom(FileNode file) throws ProjectBuildingException {
        try {
            return loadPom(file, false);
        } catch (RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

    public MavenProject loadPom(FileNode file, boolean resolve) throws RepositoryException, ProjectBuildingException {
        return loadPom(file, resolve, null, null, null);
    }

    public MavenProject loadPom(FileNode file, boolean resolve, Properties userProperties, List<String> profiles,
                                List<Dependency> dependencies) throws RepositoryException, ProjectBuildingException {
        ProjectBuildingRequest request;
        ProjectBuildingResult result;
        List<Exception> problems;

        request = new DefaultProjectBuildingRequest();
        request.setRepositorySession(repositorySession);
        request.setRemoteRepositories(remoteLegacy);
        request.setProcessPlugins(false);
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        request.setSystemProperties(System.getProperties());
        if (userProperties != null) {
            request.setUserProperties(userProperties);
        }
        //If you don't turn this into RepositoryMerging.REQUEST_DOMINANT the dependencies will be resolved against Maven Central
        //and not against the configured repositories. The default of the DefaultProjectBuildingRequest is
        // RepositoryMerging.POM_DOMINANT.
        request.setRepositoryMerging(ProjectBuildingRequest.RepositoryMerging.REQUEST_DOMINANT);
        request.setResolveDependencies(resolve);
        if (profiles != null) {
            request.setActiveProfileIds(profiles);
        }
        result = builder.build(file.toPath().toFile(), request);

        // TODO: i've seen these collection errors for a dependency with ranges. Why does Aether NOT throw an exception in this case?
        if (result.getDependencyResolutionResult() != null) {
            problems = result.getDependencyResolutionResult().getCollectionErrors();
            if (problems != null && !problems.isEmpty()) {
                throw new RepositoryException("collection errors: " + problems.toString(), problems.get(0));
            }
        }

        if (dependencies != null) {
            if (!resolve) {
                throw new IllegalArgumentException();
            }
            dependencies.addAll(result.getDependencyResolutionResult().getDependencies());
        }
        return result.getProject();
    }

    //-- versions

    /** @return Latest version field from metadata.xml of the repository that was last modified. Never null */
    public String latestVersion(Artifact artifact) throws VersionRangeResolutionException, VersionResolutionException {
        Artifact range;
        VersionRangeRequest request;
        VersionRangeResult result;
        List<Version> versions;
        String version;

        // CAUTION: do not use version "LATEST" because the respective field in metadata.xml is not set reliably:
        range = artifact.setVersion("[,]");
        request = new VersionRangeRequest(range, remote, null);
        result = repositorySystem.resolveVersionRange(repositorySession, request);
        versions = result.getVersions();
        if (versions.size() == 0) {
            throw new VersionRangeResolutionException(result, "no version found");
        }
        version = versions.get(versions.size() - 1).toString();
        if (version.endsWith("-SNAPSHOT")) {
            version = latestSnapshot(artifact.setVersion(version));
        }
        return version;
    }

    /** @return a timestamp version if a deploy artifact wins; a SNAPSHOT if a location artifact wins */
    private String latestSnapshot(Artifact artifact) throws VersionResolutionException {
        VersionRequest request;
        VersionResult result;

        request = new VersionRequest(artifact, remote, null);
        result = repositorySystem.resolveVersion(repositorySession, request);
        return result.getVersion();
    }

    public String nextVersion(Artifact artifact) throws RepositoryException {
        if (artifact.isSnapshot()) {
            return latestSnapshot(artifact.setVersion(artifact.getBaseVersion()));
        } else {
            return latestRelease(artifact);
        }
    }

    public String latestRelease(Artifact artifact) throws VersionRangeResolutionException {
        List<Version> versions;
        Version version;

        versions = availableVersions(artifact.setVersion("[" + artifact.getVersion() + ",]"));

        // ranges also return SNAPSHOTS. The elease/compatibility notes say they don't, but the respective bug
        // was re-opened: http://jira.codehaus.org/browse/MNG-3092
        for (int i = versions.size() - 1; i >= 0; i--) {
            version = versions.get(i);
            if (!version.toString().endsWith("SNAPSHOT")) {
                return version.toString();
            }
        }
        return artifact.getVersion();
    }

    public List<Version> availableVersions(String groupId, String artifactId) throws VersionRangeResolutionException {
        return availableVersions(groupId, artifactId, null);
    }

    public List<Version> availableVersions(String groupId, String artifactId, ArtifactType type) throws VersionRangeResolutionException {
        return availableVersions(new DefaultArtifact(groupId, artifactId, null, null, "[0,)", type));
    }

    public List<Version> availableVersions(Artifact artifact) throws VersionRangeResolutionException {
        VersionRangeRequest request;
        VersionRangeResult rangeResult;

        request = new VersionRangeRequest(artifact, remote, null);
        rangeResult = repositorySystem.resolveVersionRange(repositorySession, request);
        return rangeResult.getVersions();
    }

    //-- deploy

    /** convenience method */
    public void deploy(RemoteRepository target, Artifact ... artifacts) throws DeploymentException {
        deploy(target, null, Arrays.asList(artifacts));
    }

    /** convenience method */
    public void deploy(RemoteRepository target, String pluginName, Artifact ... artifacts) throws DeploymentException {
        deploy(target, pluginName, Arrays.asList(artifacts));
    }

    // CHECKSTYLE:OFF
    /**
     * You'll usually pass one jar artifact and the corresponding pom artifact.
     * @param pluginName null if you deploy normal artifacts; none-null for Maven Plugins, that you wish to add a plugin mapping for;
     *                   specifies the plugin name in this case.
     *                   See http://svn.apache.org/viewvc/maven/plugin-tools/tags/maven-plugin-tools-3.2/maven-plugin-plugin/src/main/java/org/apache/maven/plugin/plugin/metadata/AddPluginArtifactMetadataMojo.java?revision=1406624&amp;view=markup
     */
    // CHECKSTYLE:ON
    public void deploy(RemoteRepository target, String pluginName, List<Artifact> artifacts) throws DeploymentException {
        DeployRequest request;
        GroupRepositoryMetadata gm;
        String prefix;

        request = new DeployRequest();
        for (Artifact artifact : artifacts) {
            if (artifact.getFile() == null) {
                throw new IllegalArgumentException(artifact.toString() + " without file");
            }
            request.addArtifact(artifact);
            if (pluginName != null) {
                gm = new GroupRepositoryMetadata(artifact.getGroupId());
                prefix = getGoalPrefixFromArtifactId(artifact.getArtifactId());
                gm.addPluginMapping(prefix, artifact.getArtifactId(), pluginName);
                request.addMetadata(new MetadataBridge(gm));
            }
        }
        request.setRepository(target);
        repositorySystem.deploy(repositorySession, request);
    }

    /** from PluginDescriptor */
    public static String getGoalPrefixFromArtifactId(String artifactId) {
        if ("maven-plugin-plugin".equals(artifactId)) {
            return "plugin";
        } else {
            return artifactId.replaceAll("-?maven-?", "").replaceAll("-?plugin-?", "");
        }
    }

    //-- utils

    /**
     * @param globalSettings null to use default
     * @param userSettings null to use default
     */
    public static Settings loadSettings(World world, FileNode globalSettings, FileNode userSettings, DefaultPlexusContainer container)
            throws IOException, XmlPullParserException, ComponentLookupException {
        DefaultMavenSettingsBuilder builder;
        MavenExecutionRequest request;

        builder = (DefaultMavenSettingsBuilder) container.lookup(MavenSettingsBuilder.ROLE);
        request = new DefaultMavenExecutionRequest();
        if (globalSettings == null) {
            globalSettings = locateMaven(world).join("conf/settings.xml");
        }
        if (userSettings == null) {
            userSettings = (FileNode) world.getHome().join(".m2/settings.xml");
        }
        request.setGlobalSettingsFile(globalSettings.toPath().toFile());
        request.setUserSettingsFile(userSettings.toPath().toFile());
        return builder.buildSettings(request);
    }

    private static String localRepositoryPathFromMavenOpts() {
        String value;

        value = System.getenv("MAVEN_OPTS");
        if (value != null) {
            for (String entry : Separator.SPACE.split(value)) {
                if (entry.startsWith("-Dmaven.repo.local=")) {
                    return entry.substring(entry.indexOf('=') + 1);
                }
            }
        }
        return null;
    }

    public static FileNode locateMaven(World world) throws IOException {
        String home;
        FileNode mvn;

        mvn = which(world, "mvn");
        if (mvn != null) {
            mvn = mvn.getParent().getParent();
            if (mvn.join("conf").isDirectory()) {
                return mvn;
            }
        }

        home = System.getenv("MAVEN_HOME");
        if (home != null) {
            return world.file(home);
        }
        throw new IOException("cannot locate maven");
    }

    // TODO: sushi
    private static FileNode which(World world, String cmd) throws IOException {
        String path;
        FileNode file;

        path = System.getenv("PATH");
        if (path != null) {
            for (String entry : Separator.on(':').trim().split(path)) {
                file = world.file(entry).join(cmd);
                if (file.isFile()) {
                    while (file.isLink()) {
                        file = (FileNode) file.resolveLink();
                    }
                    return file;
                }
            }
        }
        return null;
    }

    private static List<ArtifactRepository> repositoriesLegacy(LegacyRepositorySystem legacy, Settings settings)
            throws InvalidRepositoryException {
        List<ArtifactRepository> result;
        List<String> actives;

        result = new ArrayList<>();
        actives = settings.getActiveProfiles();
        for (Profile profile : settings.getProfiles()) {
            if (actives.contains(profile.getId())) {
                for (org.apache.maven.model.Repository repository : SettingsUtils.convertFromSettingsProfile(profile).getRepositories()) {
                    result.add(legacy.buildArtifactRepository(repository));
                }
            }
        }
        return result;
    }
}
