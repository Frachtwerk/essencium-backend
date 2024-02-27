#!/bin/bash

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

echo ${PWD##*/};

if [ ${PWD##*/} = 'newman' ]; then
    cd ../..
fi

if [ ${PWD##*/} = 'docker' ]; then
    cd ..
fi

if [ ${PWD##*/} != 'backend' ]; then
    exit 1
fi

echo ${PWD##*/};

mvn -f essencium-backend/pom.xml clean install -Dmaven.test.skip=true
mvn -f essencium-backend-development/pom.xml clean package -DskipTests
cd essencium--backend-development/target && java -Djarmode=layertools -jar *.jar extract && cd ..

echo ${PWD##*/};

docker build . -t essencium-backend-development

cd ../docker/newman || exit 1

echo ${PWD##*/};

docker compose up -d
docker logs -f newman-newman-1
docker compose down
