FROM docker.io/library/node:26.3.0-bookworm-slim AS node

FROM docker.io/library/gradle:8.14-jdk21 AS build

RUN apt-get update \
    && apt-get install -y --no-install-recommends libatomic1 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=node /usr/local/lib/node_modules /usr/local/lib/node_modules
RUN ln -s ../lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm \
    && ln -s ../lib/node_modules/npm/bin/npx-cli.js /usr/local/bin/npx \
    && node --version \
    && npm --version

WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .

RUN --mount=type=cache,id=halalapp-gradle-deps,target=/home/gradle/.gradle,sharing=locked \
    --mount=type=cache,id=halalapp-gradle-project,target=/home/gradle/project/.gradle,sharing=locked \
    --mount=type=cache,id=halalapp-npm,target=/root/.npm,sharing=locked \
    ./gradlew :server:shadowJar --no-daemon

FROM docker.io/library/eclipse-temurin:21-jre

RUN groupadd --system --gid 10001 halalapp \
    && useradd --system --uid 10001 --gid halalapp --home-dir /app --shell /usr/sbin/nologin halalapp \
    && mkdir -p /app \
    && chown halalapp:halalapp /app

WORKDIR /app
COPY --from=build --chown=halalapp:halalapp /home/gradle/project/server/build/libs/server-all.jar app.jar

USER halalapp
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
