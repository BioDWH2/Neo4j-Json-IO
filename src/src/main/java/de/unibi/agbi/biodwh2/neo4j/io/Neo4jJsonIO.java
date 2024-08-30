package de.unibi.agbi.biodwh2.neo4j.io;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unibi.agbi.biodwh2.neo4j.io.model.CmdArgs;
import de.unibi.agbi.biodwh2.neo4j.io.model.GithubAsset;
import de.unibi.agbi.biodwh2.neo4j.io.model.GithubRelease;
import de.unibi.agbi.biodwh2.neo4j.io.model.Version;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.driver.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.Manifest;

public class Neo4jJsonIO {
    private static final Logger LOGGER = LogManager.getLogger(Neo4jJsonIO.class);
    private static final String RELEASE_URL = "https://api.github.com/repos/BioDWH2/Neo4j-Json-IO/releases";

    private Neo4jJsonIO() {
    }

    public static void main(final String... args) {
        final CmdArgs commandLine = parseCommandLine(args);
        new Neo4jJsonIO().run(commandLine);
    }

    private static CmdArgs parseCommandLine(final String... args) {
        final var result = new CmdArgs();
        final var cmd = new CommandLine(result);
        cmd.parseArgs(args);
        return result;
    }

    private void run(final CmdArgs commandLine) {
        checkForUpdate();
        if (StringUtils.isNotEmpty(commandLine.exportFilePath) && StringUtils.isNotEmpty(commandLine.endpoint))
            export(commandLine.exportFilePath, commandLine.endpoint, commandLine.username, commandLine.password);
        else {
            LOGGER.error("export and endpoint arguments must be specified");
            printHelp(commandLine);
        }
    }

    private void checkForUpdate() {
        final Version currentVersion = getCurrentVersion();
        Version mostRecentVersion = null;
        String mostRecentDownloadUrl = null;
        final ObjectMapper mapper = new ObjectMapper();
        try {
            final var releaseUrl = new URL(RELEASE_URL);
            final List<GithubRelease> releases = mapper.readValue(releaseUrl, new TypeReference<>() {
            });
            for (final GithubRelease release : releases) {
                final Version version = Version.tryParse(release.tagName.replace("v", ""));
                if (version != null) {
                    final String jarName = "Neo4j-Json-IO-" + release.tagName + ".jar";
                    final Optional<GithubAsset> jarAsset = release.assets.stream().filter(
                            asset -> asset.name.equalsIgnoreCase(jarName)).findFirst();
                    if (jarAsset.isPresent() && mostRecentVersion == null || version.compareTo(mostRecentVersion) > 0) {
                        mostRecentVersion = version;
                        //noinspection OptionalGetWithoutIsPresent
                        mostRecentDownloadUrl = jarAsset.get().browserDownloadUrl;
                    }
                }
            }
        } catch (IOException | ClassCastException ignored) {
        }
        if (currentVersion == null && mostRecentVersion != null || currentVersion != null && currentVersion.compareTo(
                mostRecentVersion) < 0) {
            LOGGER.info("=======================================");
            LOGGER.info("New version {} of Neo4j-Json-IO is available at:", mostRecentVersion);
            LOGGER.info(mostRecentDownloadUrl);
            LOGGER.info("=======================================");
        }
    }

    private Version getCurrentVersion() {
        try {
            final Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                try {
                    final var manifest = new Manifest(resources.nextElement().openStream());
                    final Version version = Version.tryParse(manifest.getMainAttributes().getValue("BioDWH2-version"));
                    if (version != null)
                        return version;
                } catch (IOException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private void printHelp(final CmdArgs commandLine) {
        CommandLine.usage(commandLine, System.out);
    }

    private void export(final String exportFilePath, final String endpoint, final String username,
                               final String password) {
        final Path exportFile = Paths.get(exportFilePath);
        try (final Driver driver = GraphDatabase.driver(endpoint, getAuthToken(username, password))) {
            try (final Session session = driver.session()) {
                // TODO
            }
        }
    }

    private AuthToken getAuthToken(final String username, final String password) {
        return StringUtils.isEmpty(username) ? AuthTokens.none() : AuthTokens.basic(username, password);
    }
}
