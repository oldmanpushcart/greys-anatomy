#!/bin/bash

# greys's target dir
GREYS_TARGET_DIR=../target/greys

# maven package the greys
mvn clean package -Dmaven.test.skip=true -f ../pom.xml
if [ ! $? -eq 0 ];then
    echo "package greys failed." >> /dev/stderr
    exit 1
fi

# reset the target dir
mkdir -p ${GREYS_TARGET_DIR}

# copy file to TARGET_DIR
cp ./greys.sh ${GREYS_TARGET_DIR}
cp -r ../scripts ${GREYS_TARGET_DIR}
cp ../core/target/greys-core-jar-with-dependencies.jar ${GREYS_TARGET_DIR}/greys-core.jar
cp ../agent/target/greys-agent-jar-with-dependencies.jar ${GREYS_TARGET_DIR}/greys-agent.jar

# make it +x
chmod +x ${GREYS_TARGET_DIR}/greys.sh

# zip the greys
cd ../target/
zip -r greys.zip greys/
cd -

