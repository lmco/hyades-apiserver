# This file is part of Dependency-Track.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
# Copyright (c) OWASP Foundation. All Rights Reserved.

# Arguments that can be passed at build time
# Directory names must end with / to avoid errors when COPYing
ARG COMMIT_SHA=unknown
ARG APP_VERSION=0.0.0
ARG APP_DIR=/opt/owasp/dependency-track/
ARG DATA_DIR=/data/
ARG UID=1000
ARG GID=1000

# Using a non-alpine version unlike open source hyades
FROM eclipse-temurin@sha256:cde9b1a7777579c5b443e86146d30b5f730c0f047762ee810672e3d89f9da646 AS jre-build

WORKDIR /work

COPY --chmod=755 ./src/main/docker/create-jre.sh ./
COPY ./target/lib/ ./lib/
COPY ./target/dependency-track-apiserver.jar ./

RUN ./create-jre.sh -i ./dependency-track-apiserver.jar -o ./jre

# Open source hyades is using an alpine container, but that complicates things
FROM debian:bookworm

ARG COMMIT_SHA
ARG APP_VERSION
ARG APP_DIR
ARG DATA_DIR
ARG UID
ARG GID

ENV TZ=Etc/UTC \
    # JVM Options that are passed at runtime by default
    JAVA_OPTIONS="-XX:+UseG1GC -XX:+UseStringDeduplication -XX:+UseCompactObjectHeaders -XX:MaxGCPauseMillis=250 -XX:MaxRAMPercentage=80.0" \
    # JVM Options that can be passed at runtime, while maintaining also those set in JAVA_OPTIONS.
    # e.g. EXTRA_JAVA_OPTIONS="-Dlogback.configurationFile=logback-json.xml"
    EXTRA_JAVA_OPTIONS="" \
    # The web context defaults to the root. To override, supply an alternative context which starts with a / but does not end with one
    # Example: /dtrack
    CONTEXT="/" \
    JAVA_HOME=/opt/java/openjdk \
    PATH="/opt/java/openjdk/bin:${PATH}" \
    LANG=C.UTF-8 \
    # Ensure user home is always set to DATA_DIR, even for arbitrary UIDs (such as used by OpenShift)
    HOME=${DATA_DIR} \
    # Default notification publisher templates override environment variables
    DEFAULT_TEMPLATES_OVERRIDE_ENABLED=false \
    DEFAULT_TEMPLATES_OVERRIDE_BASE_DIRECTORY=${DATA_DIR} \
    # LM Proxy
    http_proxy="http://proxy-zsgov.external.lmco.com:80" \
    HTTP_PROXY="$http_proxy" \
    https_proxy="http://proxy-zsgov.external.lmco.com:80" \
    HTTPS_PROXY="$https_proxy" \
    no_proxy="lmco.com,localdomain,localhost,127.0.0.1,.amazonaws.com,.lmco.com" \
    NO_PROXY="$no_proxy"

# LM Certs
COPY /src/main/docker/Combined_pem.pem /usr/local/share/ca-certificates/Combined_pem.crt

RUN apt-get update \
    && apt-get install -y ca-certificates curl \
    && update-ca-certificates \
    && install -m 0755 -d /etc/apt/keyrings \
    && curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc \
    && chmod a+r /etc/apt/keyrings/docker.asc \
    && echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo \"$VERSION_CODENAME\") stable" | \
        tee /etc/apt/sources.list.d/docker.list > /dev/null \
    && apt-get update \
    && apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Create the directories where the JAR will be deployed to (${APP_DIR}) and Dependency-Track will store its data (${DATA_DIR})
# Create a user and assign home directory to a ${DATA_DIR}
# Ensure UID 1000 & GID 1000 own all the needed directories
# Short flags are ambiguous, so use the fully qualified flags
RUN mkdir -p ${APP_DIR} ${DATA_DIR} \
    && addgroup --system --gid ${GID} dtrack \
    && adduser --system --disabled-login --no-create-home --home ${DATA_DIR} --gecos "dtrack user"  --shell /bin/false -u ${UID} dtrack-user \
    && chown -R dtrack-user:root ${DATA_DIR} ${APP_DIR} \
    && chmod -R g=u ${DATA_DIR} ${APP_DIR}

WORKDIR ${APP_DIR}
USER ${UID}

COPY --from=jre-build --chown=${UID}:0 /work/jre ${JAVA_HOME}
COPY --chown=${UID}:0 ./target/lib/ ./lib/
COPY --chown=${UID}:0 ./target/dependency-track-apiserver.jar ./src/main/docker/logback-json.xml ./

# Launch Dependency-Track
CMD [ \
    "/bin/sh", "-c", \
    "exec java \
        ${JAVA_OPTIONS} \
        ${EXTRA_JAVA_OPTIONS} \
        --add-opens java.base/java.util.concurrent=ALL-UNNAMED \
        --enable-native-access=ALL-UNNAMED \
        --sun-misc-unsafe-memory-access=allow \
        -cp 'dependency-track-apiserver.jar:lib/*' \
        org.dependencytrack.Application \
        -context ${CONTEXT}" \
]

# Specify which ports Dependency-Track listens on
EXPOSE 8080
EXPOSE 9000

# Health check against the management port
# Health check doesn't seem to work with podman, so commenting out for now
# HEALTHCHECK --interval=30s --start-period=60s --timeout=3s CMD [ \
#     "/bin/sh", "-c", \
#     "curl -f -s --max-time 3 --noproxy '*' -o /dev/null http://127.0.0.1:9000/health" \
# ]

# metadata labels
LABEL \
    org.opencontainers.image.vendor="OWASP" \
    org.opencontainers.image.title="Official Dependency-Track Container image" \
    org.opencontainers.image.description="Dependency-Track is an intelligent Component Analysis platform" \
    org.opencontainers.image.version="${APP_VERSION}" \
    org.opencontainers.image.url="https://dependencytrack.org/" \
    org.opencontainers.image.source="https://github.com/DependencyTrack/dependency-track" \
    org.opencontainers.image.revision="${COMMIT_SHA}" \
    org.opencontainers.image.licenses="Apache-2.0" \
    maintainer="steve.springett@owasp.org"
