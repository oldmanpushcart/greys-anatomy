#!/bin/bash

# define newset greys's version
# need ./greys-packages.sh replace the version number
GREYS_VERSION=0.0.0.0

# define newset greys lib home
GREYS_LIB_HOME=${HOME}/.greys/lib/${GREYS_VERSION}/greys

# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err()
{
    [[ ! -z "${2}" ]] && echo "${2}" >> /dev/stderr
    exit ${1}
}

# install to local if necessary
if [[ ! -x ${GREYS_LIB_HOME} ]]; then

    # install to local
    mkdir -p ${GREYS_LIB_HOME} \
    || exit_on_err 1 "create target directory ${GREYS_LIB_HOME} failed."

    # copy jar files
    cp *.jar ${GREYS_LIB_HOME}/

fi

echo "install to local successed."

