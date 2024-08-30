package de.unibi.agbi.biodwh2.neo4j.io.model;

import picocli.CommandLine;

@CommandLine.Command(name = "Neo4j-Json-IO.jar", sortOptions = false, footer = "Visit https://github.com/BioDWH2/Neo4j-Json-IO for more documentation.")
public class CmdArgs {
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "print this message", order = 0)
    public boolean help;
    @CommandLine.Option(names = {
            "--export"
    }, arity = "1", paramLabel = "<filePath>", description = "Path to the exported file", order = 1)
    public String exportFilePath;
    @CommandLine.Option(names = {
            "-e", "--endpoint"
    }, arity = "1", paramLabel = "<endpoint>", description = "Endpoint of a running Neo4j instance", order = 2)
    public String endpoint;
    @CommandLine.Option(names = {
            "--username"
    }, arity = "1", paramLabel = "<username>", description = "Neo4j username", order = 3)
    public String username;
    @CommandLine.Option(names = {
            "--password"
    }, arity = "1", paramLabel = "<password>", description = "Neo4j password", order = 4)
    public String password;
}
