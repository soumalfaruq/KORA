FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x ./gradlew && ./gradlew --no-daemon :app:installDist

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home appuser
COPY --from=build --chown=10001:10001 /workspace/app/build/install/app/ /app/
USER 10001
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["/app/bin/app"]
