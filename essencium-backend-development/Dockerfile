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

FROM amazoncorretto:17

LABEL maintainer="Frachtwerk GmbH"

ENV TZ=Europe/Berlin

COPY /target/dependencies/ ./
COPY /target/spring-boot-loader/ ./
#COPY /target/snapshot-dependencies/ ./ # currently not available in the build process in github actions
COPY /target/application/ ./

HEALTHCHECK --interval=60s --timeout=30s --start-period=30s --retries=3 CMD if [ "$(curl -f http://localhost:8098/actuator/health)" != '{"status":"UP"}' ]; then exit 1; fi

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom org.springframework.boot.loader.launch.JarLauncher" ]
