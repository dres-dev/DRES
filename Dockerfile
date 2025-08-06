FROM zenika/kotlin:1.4.20-jdk11 AS build

COPY . /dres-src
RUN cd /dres-src && \
  ./gradlew distTar && \
  mkdir dres-dist && \
  cd dres-dist && \
  tar xf ../backend/build/distributions/dres-dist.tar

FROM zenika/kotlin:1.4.20-jdk11-slim

RUN mkdir /dres-data
COPY config.json /dres-data
COPY --from=build /dres-src/dres-dist /

EXPOSE 8080
EXPOSE 8443
ENTRYPOINT [ "sh", "-c", "ls -l /dres-dist/lib/ffmpeg && chmod +x /dres-dist/lib/ffmpeg/ffmpeg && chmod +x /dres-dist/lib/ffmpeg/ffprobe && /dres-dist/bin/backend /dres-data/config.json" ]