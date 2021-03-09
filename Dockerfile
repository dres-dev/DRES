FROM zenika/kotlin:1.4.20-jdk11 AS build

COPY . /dres-src
RUN cd /dres-src && \
  ./gradlew distTar


RUN mkdir dres-dist && \
  cd dres-dist && \
  tar xf ../backend/build/distributions/dres-dist.tar

FROM zenika/kotlin:1.4.20-jdk11-slim

RUN mkdir /dres-data
COPY backend/config.json /dres-data
COPY --from=build /dres-src/dres-dist /

EXPOSE 8080
EXPOSE 8443
ENTRYPOINT /dres-dist/bin/dres /dres-data/config.json