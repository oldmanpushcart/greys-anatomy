#!/bin/bash

# program : install-local.sh
#           install greys by local
#  author : oldmanpushcart@gmail.com
#    date : 2018-03-12
# version : 2.0.0.0

# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err()
{
    [[ ! -z "${2}" ]] && echo "${2}" 1>&2
    exit ${1}
}

# define newest greys's version
# need ./greys-packages.sh replace the version number
GREYS_VERSION=0.0.0.0
[[ ${GREYS_VERSION} == "0.0.0.0" ]] \
    && exit_on_err 1 "not init yet!"

# define newest greys lib home
INSTALL_TARGET_DIR=${HOME}/.greys2/${GREYS_VERSION}/greys

mkdir -p ${INSTALL_TARGET_DIR} \
    && cp -r * ${INSTALL_TARGET_DIR} \
    || exit_on_err 1 "local install greys fail."

echo "version : ${GREYS_VERSION}"
echo "   path : ${INSTALL_TARGET_DIR}"
echo "install greys by local success."

