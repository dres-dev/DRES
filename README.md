# DRES

[![swagger-editor](https://img.shields.io/badge/open--API-in--editor-brightgreen.svg?style=flat&label=open-api-v3)](https://editor.swagger.io/?url=https://raw.githubusercontent.com/lucaro/DRES/master/doc/openapi-v3.json)

The Distributed Retrieval Evaluation Server builds uppon the work of https://github.com/klschoef/vbsserver/ to provide the means to evaluate interactive retrieval approaches in various settings, both on-site and distributed.

## Requirements

To deploy and run DRES, a JRE of at least Java 11 is required, e.g. the [OpenJDK 11](https://jdk.java.net/java-se-ri/11).
For development, besides the JDK, addtitionally [NPM](https://www.npmjs.com/) and the [Angular CLI](https://cli.angular.io/) is recommended.

## Setup

DRES consists of two components: The [backend](backend/) and the [frontend](frontend/), each subdirectories of this repository.
The backend is written in Kotlin, hence requires a JVM, and the frontend is written in Angular using Angular-CLI.
However, the entire setup process is Gradle based. To setup DRES, follow these steps:

1. Clone or Download this repository
2. Navigate to the [backend](backend/)
3. Setup FFMpeg using Gradle `$> ./gradlew setupFFMpeg`
4. Build the frontend using Gradle `$> ./gradlew deployFrontend`
5. Build the backend using Gradle `$> ./gradlew distZip` (alternatively `$> ./gradlew distTar`)
6. Extract and run the backend (it serves the frontend)

## Configuration

Both the backend and the frontend have several properties which can be configured using a json configuration file.

### Backend Configuration

The backend looks for a file named `config.json` at the location from where it is started. If this file is not found, default values are used for all configurable settings. To use a different configutation file, pass its path as the first parameter when starting the backend. The configuration file is structured as follows. All values are optional and overwrite the defaults listed below, if set.

```json5

{
  "httpPort":         8080,           //the port to be used for HTTP
  "httpsPort":        8443,           //the port to be used for HTTPS, if enabled
  "enableSsl":        true,           //specifies if HTTPS is supposed to be enabled. If set to true, HTTP connections will be upgraded
  "keystorePath":     "keystore.jks", //path to the Java Key Store file to be used for the SSL certificate
  "keystorePassword": "password",     //password for the Key Store file
  "externalPath":     "./external",   //path for external media items
  "dataPath":         "./data",       //path where the local database is to be stored
  "cachePath":        "./cache"       //path where pre-rendered media items are to be stored
}

```

## First Steps

Using the DRES CLI, the `help` command lists available commands and their usage. If one would like to know more about a certain command, use the argument `-h`.
Following the first steps towards a successful installment of a (distributed) retrieval evaluation campagn. A prerequisit is the previous deployment, see [Setup](#setup) and a running DRES instance.

### Create User

Using the CLI, the following command creates your first _administrator_ user with the username `admin` and the password `password` (**Do not use this user in production**).

```
DRES> user create -u admin -p password -r ADMIN
```

Additional users are required in order to perform an evaluation. At least one user per participating system instance.

### Create Media Collection

Again, using the CLI one can create the very first media collection, called `first`.
This assumes you already have a folder with multimedia content for the collection at `path/to/media`.


```
DRES> collection create -n first -d "A desription of the collection" -p path/to/media
```

Then, there are several ways to import the actual multimedia content. The easiest way (but might take a while) is to use the scan command:

```
DRES> collection scan -c first
```

### Create Competition

The next step is to create a competition. This is done using the web frontend of DRES.
Using your browser, navigate to your DRES address and perform the login using the administrator user created previously.

Then, navigate to _Competition Builder_ and create a new competition. Follow the instructions there.

### Competition Run

_TODO_

## Submission
In order to submit a result to be evaluated, the submission endpoint is accessed via HTTP(S) in one of the following ways:
- http(s)://{server}/submit?*item*={item} where {item} is the identifier for the retrieved media item
- http(s)://{server}/submit?*item*={item}?*shot*={shot} where {shot} is the identifier for a pre-defined temporal segment within the {item}
- http(s)://{server}/submit?*item*={item}?*frame*={frame} where {frame} is the frame number within the {item}, in case it is a video
- http(s)://{server}/submit?*item*={item}?*timecode*={timecode} where {timecode} is a temporal position within the {item} in the form HH:MM:SS:FF. In case just a plain number is passed, the behavior is equivalent to passing the same value as {frame}
In case no session cookie is passed as part of the request, an additional *session* parameter can be passed to transmit the session id token. The session id can be found via the UI in the User Profile, accessible via the top-right menu.

## Interaction Logging
Analogously to the VBS server, the logging of interaction sequences and complete result lists is supported. The specification of the messages can be found in [this working document](https://www.overleaf.com/read/rppygxshvhrn) as well as the OpenApi specifications. The interaction/querying logs can be submittet to http(s)://{server}/log/query and the result logs can be sent to http(s)://{server}/log/result, both via POST.

## Known Issues

### Backend

_none_

### Frontend

**Safari is NOT supported**
