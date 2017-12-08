#!/bin/bash

BASEDIR="$( cd "$( dirname "$0" )" && pwd )"

# greys's target dir
GREYS_TARGET_DIR=${BASEDIR}/../target/greys

# greys's version
GREYS_VERSION=$(cat ${BASEDIR}/..//core/src/main/resources/com/github/ompc/greys/core/res/version)

# define newset greys lib home
NEWEST_GREYS_LIB_HOME=${HOME}/.greys/lib/${GREYS_VERSION}/greys


# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err()
{
    [[ ! -z "${2}" ]] && echo "${2}" 1>&2
    exit ${1}
}

# maven package the greys
mvn clean package -Dmaven.test.skip=true -f ${BASEDIR}/../pom.xml \
|| exit_on_err 1 "package greys failed."

# reset the target dir
mkdir -p ${GREYS_TARGET_DIR}

# copy jar to TARGET_DIR
cp ${BASEDIR}/../core/target/greys-core-jar-with-dependencies.jar ${GREYS_TARGET_DIR}/greys-core.jar
cp ${BASEDIR}/../agent/target/greys-agent-jar-with-dependencies.jar ${GREYS_TARGET_DIR}/greys-agent.jar

# copy shell to TARGET_DIR
cat ${BASEDIR}/install-local.sh|sed "s/GREYS_VERSION=0.0.0.0/GREYS_VERSION=${GREYS_VERSION}/g" > ${GREYS_TARGET_DIR}/install-local.sh
#chmod +x ${GREYS_TARGET_DIR}/install-local.sh
cp ${BASEDIR}/greys.sh ${GREYS_TARGET_DIR}/greys.sh
cp ${BASEDIR}/ga.sh ${GREYS_TARGET_DIR}/ga.sh
cp ${BASEDIR}/gs.sh ${GREYS_TARGET_DIR}/gs.sh
chmod +x ${GREYS_TARGET_DIR}/*.sh

# zip/tar the greys
cd ${BASEDIR}/../target/
zip -r greys-${GREYS_VERSION}-bin.zip greys/
tar -cvf ./greys-${GREYS_VERSION}-bin.tar ./greys
cd -

# install to local
mkdir -p ${NEWEST_GREYS_LIB_HOME}
cp ${BASEDIR}/../target/greys/* ${NEWEST_GREYS_LIB_HOME}/