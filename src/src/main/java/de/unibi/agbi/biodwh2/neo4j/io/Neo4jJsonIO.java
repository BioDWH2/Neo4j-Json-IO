package de.unibi.agbi.biodwh2.neo4j.io;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.neo4j.driver.types.Entity;
import picocli.CommandLine;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.Manifest;
import java.util.zip.GZIPOutputStream;

public class Neo4jJsonIO {
    private static final Logger LOGGER = LogManager.getLogger(Neo4jJsonIO.class);
    private static final String RELEASE_URL = "https://api.github.com/repos/BioDWH2/Neo4j-Json-IO/releases";
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
        if (commandLine.help) {
            printHelp(commandLine);
        }
        else if (StringUtils.isNotEmpty(commandLine.exportFilePath) && StringUtils.isNotEmpty(commandLine.endpoint))
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
        try {
            final var releaseUrl = new URL(RELEASE_URL);
            final List<GithubRelease> releases = MAPPER.readValue(releaseUrl, new TypeReference<>() {
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
        try (final OutputStream stream = new GZIPOutputStream(Files.newOutputStream(exportFile));
             final Driver driver = GraphDatabase.driver(endpoint, getAuthToken(username, password))) {
            try (final Session session = driver.session()) {
                final var relationshipTypes = session.run("CALL db.relationshipTypes").stream().map(
                        l -> l.get(0).asString()).toArray(String[]::new);
                final var writer = new PrintWriter(stream, true, StandardCharsets.UTF_8);
                writer.println("{");
                writer.println("  \"graph\": {");
                writer.println("    \"nodes\": {");
                boolean first = true;
                {
                    final var result = session.run("MATCH (n) RETURN n");
                    while (result.hasNext()) {
                        final var record = result.next();
                        final var node = record.get(0).asNode();
                        if (!first)
                            writer.println(",");
                        first = false;
                        writer.println("      \"" + node.elementId() + "\": {");
                        writer.println("        \"label\": \"" + String.join(";", node.labels()) + "\",");
                        writer.println("        \"metadata\": " + createMetadata(node));
                        writer.print("      }");
                    }
                }
                if (!first)
                    writer.println();
                writer.println("    },");
                writer.println("    \"edges\": [");
                first = true;
                for (final String label : relationshipTypes) {
                    final var result = session.run("MATCH ()-[r:" + label + "]->() RETURN r");
                    while (result.hasNext()) {
                        final var record = result.next();
                        final var edge = record.get(0).asRelationship();
                        if (!first)
                            writer.println(",");
                        first = false;
                        writer.println("      {");
                        writer.println("        \"id\": \"" + edge.elementId() + "\",");
                        writer.println("        \"source\": \"" + edge.startNodeElementId() + "\",");
                        writer.println("        \"target\": \"" + edge.endNodeElementId() + "\",");
                        writer.println("        \"label\": \"" + label + "\",");
                        writer.println("        \"metadata\": " + createMetadata(edge));
                        writer.print("      }");
                    }
                }
                if (!first)
                    writer.println();
                writer.println("    ]");
                writer.println("  }");
                writer.println("}");
            }
        } catch (IOException ex) {
            LOGGER.error("Failed to export", ex);
        }
    }

    private AuthToken getAuthToken(final String username, final String password) {
        return StringUtils.isEmpty(username) ? AuthTokens.none() : AuthTokens.basic(username, password);
    }

    private String createMetadata(final Entity entity) throws JsonProcessingException {
        final Map<String, Object> properties = new HashMap<>();
        for (final var key : entity.keys()) {
            final var value = entity.get(key);
            properties.put(key, value.asObject());
        }
        return MAPPER.writeValueAsString(properties);
    }
}
