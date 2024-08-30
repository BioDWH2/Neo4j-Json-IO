![Java CI](https://github.com/BioDWH2/Neo4j-Json-IO/workflows/Java%20CI/badge.svg?branch=develop) ![Release](https://img.shields.io/github/v/release/BioDWH2/Neo4j-Json-IO) ![Downloads](https://img.shields.io/github/downloads/BioDWH2/Neo4j-Json-IO/total) ![License](https://img.shields.io/github/license/BioDWH2/Neo4j-Json-IO)

# Neo4j-Json-IO
This repository contains the **Neo4j-Json-IO** utility for exporting Neo4j databases into JsonGraph format.

## Download
The latest release version of **Neo4j-Json-IO** can be downloaded [here](https://github.com/BioDWH2/Neo4j-Json-IO/releases/latest).

## Usage
Neo4j-Json-IO requires the Java Runtime Environment version 16 or higher. The JRE 16 is available [here](https://adoptopenjdk.net/releases.html?variant=openjdk16).

Exporting a `.json.gz` file from Neo4j is done using the following command.
~~~BASH
> java -jar Neo4j-Json-IO.jar --export /path/to/file.json.gz -e bolt://localhost:8083 --username user --password pass
~~~

If authentication is disabled, the username and password parameters can be ignored.
~~~BASH
> java -jar Neo4j-GraphML-Importer.jar -i /path/to/file.graphml -e bolt://localhost:8083
~~~

## Help
~~~
Usage: Neo4j-Json-IO.jar [-h] [-e=<endpoint>] [--export=<filePath>]
                         [--password=<password>] [--username=<username>]
  -h, --help                print this message
      --export=<filePath>   Path to the exported file
  -e, --endpoint=<endpoint> Endpoint of a running Neo4j instance
      --username=<username> Neo4j username
      --password=<password> Neo4j password
~~~