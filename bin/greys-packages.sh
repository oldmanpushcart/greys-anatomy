#!/bin/bash

# greys's target dir
GREYS_TARGET_DIR=../target/greys

# greys's version
GREYS_VERSION=$(cat ..//core/src/main/resources/com/github/ompc/greys/core/res/version)

# define newset greys lib home
NEWEST_GREYS_LIB_HOME=${HOME}/.greys/lib/${GREYS_VERSION}/greys


# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err()
{
    [[ ! -z "${2}" ]] && echo "${2}" >> /dev/stderr
    exit ${1}
}

# maven package the greys
mvn clean package -Dmaven.test.skip=true -f ../pom.xml \
|| exit_on_err 1 "package greys failed."

# reset the target dir
mkdir -p ${GREYS_TARGET_DIR}

# copy jar to TARGET_DIR
cp ../core/target/greys-core-jar-with-dependencies.jar ${GREYS_TARGET_DIR}/greys-core.jar
cp ../agent/target/greys-agent-jar-with-dependencies.jar ${GREYS_TARGET_DIR}/greys-agent.jar

# copy shell to TARGET_DIR
cat install-local.sh|sed "s/GREYS_VERSION=0.0.0.0/GREYS_VERSION=${GREYS_VERSION}/g" > ${GREYS_TARGET_DIR}/install-local.sh
chmod +x ${GREYS_TARGET_DIR}/install-local.sh
cp greys.sh ${GREYS_TARGET_DIR}/greys.sh

# zip the greys
cd ../target/
zip -r greys-${GREYS_VERSION}.zip greys/
cd -

# install to local
mkdir -p ${NEWEST_GREYS_LIB_HOME}
cp ../target/greys/* ${NEWEST_GREYS_LIB_HOME}/