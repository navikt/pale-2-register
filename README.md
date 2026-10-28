[![Build status](https://github.com/navikt/pale-2-register/workflows/Deploy%20to%20dev%20and%20prod/badge.svg)](https://github.com/navikt/pale-2-register/workflows/Deploy%20to%20dev%20and%20prod/badge.svg)

# Persisting of legeerklæringer
Application for persisting legeerklæringer in database

## FlowChart
This the high level flow for the application
```mermaid
  graph LR
      A[\teamsykmelding.legeerklaering/] --> pale-2-register
      pale-2-register <--> id1[(Database)]
      pale-2-register --- GCP-Bucket;
```

## Technologies used
* Kotlin
* Ktor
* Gradle
* Junit
* Jackson
* Postgres

## Requirements for developing the application
#### Java
Make sure you have the Java JDK 25 installed
You can check which version you have installed using this command:
``` bash
java -version
```
or install it: https://sdkman.io/

#### Docker
Make sure you have docker installed using this command:
You can check which version you have installed using this command:
``` bash
docker --version
```
or install it: https://docs.docker.com/engine/install/


### Building the application
#### Compile and package application
To build locally and run the integration tests you can simply run
``` bash
./gradlew installDist
```
or on windows
`gradlew.bat installDist`

### Creating a docker image
Creating a docker image should be as simple as `docker build -t pale-2-register .`

### Running a docker image
`docker run --rm -it -p 8080:8080 pale-2-register`
> **Note**
> There are many services and environment variables that need to be set up, for the application to work properly.


### Access to the Postgres database

For information on connecting to dev og prod DB see: [Postgres GCP](https://doc.nais.io/cli/commands/postgres/)

### Upgrading the Gradle wrapper version to latest
``` bash
./gradlew :wrapper --gradle-version latest
```

### Contact

This project is maintained by [navikt/teamsykmelding](CODEOWNERS)

Questions and/or feature requests? Please create an [issue](https://github.com/navikt/pale-2-register/issues)

If you work in [@navikt](https://github.com/navikt) you can reach us at the Slack
channel [#team-sykmelding](https://nav-it.slack.com/archives/CMA3XV997)
