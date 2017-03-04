#!/bin/bash

# greys's target dir
GREYS_TARGET_DIR=../target/greys

# greys's version
GREYS_VERSION=$(cat ..//greys-core/src/main/resources/com/github/ompc/greys/core/res/version)

# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err()
{
    [[ ! -z "${2}" ]] && echo "${2}" 1>&2
    exit ${1}
}

echo "prepare package greys version=${GREYS_VERSION}"

# maven package the greys
mvn clean package -Dmaven.test.skip=true -f ../pom.xml \
|| exit_on_err 1 "package greys failed."

# reset the target dir
mkdir -p ${GREYS_TARGET_DIR} \
    && cp ../greys-core/target/greys-core-*-jar-with-dependencies.jar ${GREYS_TARGET_DIR}/greys-module.jar


# install newest greys
mkdir -p ~/.sandbox-module/ \
    && cp ${GREYS_TARGET_DIR}/greys-module.jar ~/.sandbox-module/ \
    || exit_on_err 1 "install newest greys failed."