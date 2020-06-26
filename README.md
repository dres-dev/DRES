# DRES
The Distributed Retrieval Evaluation Server builds uppon the work of https://github.com/klschoef/vbsserver/ to provide the means to evaluate interactive retrieval approaches in various settings, both on-site and distributed.

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

## Submission
In order to submit a result to be evaluated, the submission endpoint is accessed via HTTP(S) in one of the following ways:
- http(s)://{server}/submit?*item*={item} where {item} is the identifier for the retrieved media item
- http(s)://{server}/submit?*item*={item}?*shot*={shot} where {shot} is the identifier for a pre-defined temporal segment within the {item}
- http(s)://{server}/submit?*item*={item}?*frame*={frame} where {frame} is the frame number within the {item}, in case it is a video
- http(s)://{server}/submit?*item*={item}?*timecode*={timecode} where {timecode} is a temporal position within the {item} in the form HH:MM:SS:FF. In case just a plain number is passed, the behavior is equivalent to passing the same value as {frame}
In case no session cookie is passed as part of the request, an additional *session* parameter can be passed to transmit the session token.
