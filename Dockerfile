FROM docker.io/library/eclipse-temurin:17-jdk AS build

COPY . /dres-src
RUN cd /dres-src && \
  ./gradlew --no-daemon distTar && \
  tar xf backend/build/distributions/dres-dist.tar -C /opt

FROM docker.io/library/eclipse-temurin:17-jre

RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg && rm -rf /var/lib/apt/lists/*

RUN mkdir /dres-data
COPY config.json /dres-data
COPY --from=build /opt/dres-dist /dres-dist

EXPOSE 8080
EXPOSE 8443
# Keep detached containers alive when stdin is closed, while preserving interactive stdin.
ENTRYPOINT ["/bin/bash", "-c", "if [ -t 0 ] || [ -p /dev/stdin ] || [ -f /dev/stdin ]; then exec /dres-dist/bin/backend /dres-data/config.json; else exec /dres-dist/bin/backend /dres-data/config.json < <(tail -f /dev/null); fi"]