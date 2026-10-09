FROM docker.io/library/eclipse-temurin:17-jdk AS build

COPY . /dres-src
RUN cd /dres-src && \
  ./gradlew --no-daemon distTar && \
  tar xf backend/build/distributions/dres-dist.tar -C /opt

FROM docker.io/library/eclipse-temurin:17-jre

RUN mkdir /dres-data
COPY config.json /dres-data
COPY --from=build /opt/dres-dist /dres-dist

EXPOSE 8080
EXPOSE 8443
# When stdin is a terminal or an open pipe (docker run -it / -i) the interactive
# CLI works as usual. With detached containers stdin is /dev/null, JLine hits EOF
# and the CLI loop would shut the server down; hold a quiet pipe open on stdin
# instead so DRES keeps serving.
ENTRYPOINT ["/bin/bash", "-c", "if [ -t 0 ] || [ -p /dev/stdin ] || [ -f /dev/stdin ]; then exec /dres-dist/bin/backend /dres-data/config.json; else exec 0< <(tail -f /dev/null); exec /dres-dist/bin/backend /dres-data/config.json; fi"]