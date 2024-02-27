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

if [ ${PWD##*/} = 'docker' ]; then
    cd ..
fi

mvn -f essencium-backend/pom.xml clean install -Dmaven.test.skip=true -Dgpg.skip=true
mvn -f essencium-backend-development/pom.xml clean package -DskipTests -Dgpg.skip=true
cd essencium-backend-development/target && java -Djarmode=layertools -jar *.jar extract && cd ..

docker build -t essencium-backend-development .

echo "Now you can run: docker compose up -d"
