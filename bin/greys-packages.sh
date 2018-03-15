#!/bin/bash

# program : greys-packages.sh
#           package the greys release zip file
#  author : oldmanpushcart@gmail.com
#    date : 2018-03-12
# version : 2.0.0.0

# set -x

BASEDIR="$( cd "$( dirname "$0" )" && pwd )"

# greys dependent jvm-sandbox's version
SANDBOX_VERSION="0.1.0.7"

# greys's version
GREYS_VERSION=$(cat ${BASEDIR}/..//greys-module/src/main/resources/com/github/ompc/greys/module/res/version)

# greys's target dir
GREYS_TARGET_DIR=${BASEDIR}/../target/greys

# download jvm-sandbox timeout(sec)
SO_TIMEOUT_SEC=60

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
    || exit_on_err 1 "mvn package greys fail."

# copy greys's file to TARGET_DIR
mkdir -p ${GREYS_TARGET_DIR} \
    && cp ${BASEDIR}/../greys-console/target/greys-console-*-jar-with-dependencies.jar \
          ${GREYS_TARGET_DIR}/greys-console.jar \
    && cp ${BASEDIR}/../template/template-greys.sh \
          ${GREYS_TARGET_DIR}/greys.sh \
    && cat ${BASEDIR}/../template/template-install-local.sh \
        | sed "s/GREYS_VERSION=0.0.0.0/GREYS_VERSION=${GREYS_VERSION}/g" \
        > ${GREYS_TARGET_DIR}/install-local.sh \
    && chmod +x ${GREYS_TARGET_DIR}/*.sh \
    && echo "copy greys's file finished." \
    || exit_on_err 1 "copy greys's file fail."


# install jvm-sandbox's to TARGET_DIR
curl -#Lk --connect-timeout ${SO_TIMEOUT_SEC} \
        -o ${GREYS_TARGET_DIR}/sandbox-${SANDBOX_VERSION}-bin.zip \
        "http://ompc.oss.aliyuncs.com/jvm-sandbox/release/sandbox-${SANDBOX_VERSION}-bin.zip" \
    && unzip ${GREYS_TARGET_DIR}/sandbox-${SANDBOX_VERSION}-bin.zip -d ${GREYS_TARGET_DIR} \
    && rm ${GREYS_TARGET_DIR}/sandbox-${SANDBOX_VERSION}-bin.zip \
    && rm -rf ${GREYS_TARGET_DIR}/sandbox/example \
    && rm -rf ${GREYS_TARGET_DIR}/sandbox/install-local.sh \
    && cp ${BASEDIR}/../template/template-sandbox.properties \
          ${GREYS_TARGET_DIR}/sandbox/cfg/sandbox.properties \
    && cp ${BASEDIR}/../template/template-sandbox-logback.xml \
          ${GREYS_TARGET_DIR}/sandbox/cfg/sandbox-logback.xml \
    && cp ${BASEDIR}/../greys-module/target/greys-module-*-jar-with-dependencies.jar \
          ${GREYS_TARGET_DIR}/sandbox/module/greys-module.jar \
    && echo "install jvm-sandbox finished." \
    || exit_on_err 1 "install jvm-sandbox fail."


# zip the greys
cd ${BASEDIR}/../target/ \
    && zip -r greys-${GREYS_VERSION}-bin.zip greys/ \
    && cd - \
    && echo "package greys finished. file:${GREYS_TARGET_DIR}/../greys-${GREYS_VERSION}-bin.zip" \
    || exit_on_err 1 "package greys fail."




# install to local
# define newest greys lib home
## NEWEST_GREYS_LIB_HOME=${HOME}/.greys/lib/${GREYS_VERSION}/greys
## mkdir -p ${NEWEST_GREYS_LIB_HOME}
## cp ${BASEDIR}/../target/greys/* ${NEWEST_GREYS_LIB_HOME}/