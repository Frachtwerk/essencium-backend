# Copyright (C) 2024 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
#
# This file is part of essencium-backend.
#
# essencium-backend is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# essencium-backend is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with essencium-backend. If not, see <http://www.gnu.org/licenses/>.

FROM eclipse-temurin:17-jre

LABEL maintainer="Frachtwerk GmbH"

ENV TZ=Europe/Berlin

RUN useradd user

USER user

COPY /target/extracted/dependencies/ ./
COPY /target/extracted/spring-boot-loader/ ./
COPY /target/extracted/snapshot-dependencies/ ./
COPY /target/extracted/application/ ./

EXPOSE 8098

HEALTHCHECK --interval=60s --timeout=30s --start-period=30s --retries=3 CMD curl --fail --silent localhost:8098/actuator/health | grep UP || exit 1

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom org.springframework.boot.loader.launch.JarLauncher" ]
