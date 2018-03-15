#! /bin/bash

# program : install.sh
#           install greys by online
#  author : oldmanpushcart@gmail.com
#    date : 2018-03-12
# version : 2.0.0.0

# temp file of greys.sh
TEMP_GREYS_FILE="./greys.sh.$$"

# target file of greys.sh
TARGET_GREYS_FILE="./greys.sh"

# update timeout(sec)
SO_TIMEOUT_SEC=60

# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err()
{
    [[ ! -z "${2}" ]] && echo "${2}" 1>&2
    exit ${1}
}

# check permission to download && install
[ ! -w ./ ] \
    && exit_on_err 1 "permission denied. directory ./ is not writable."

# download greys.sh from server
curl -#Lk --connect-timeout ${SO_TIMEOUT_SEC} \
        "http://ompc.oss.aliyuncs.com/greys2/greys.sh" \
        -o ${TEMP_GREYS_FILE} \
    && rm -rf greys.sh \
    && mv ${TEMP_GREYS_FILE} ${TARGET_GREYS_FILE} \
    && chmod +x ${TARGET_GREYS_FILE} \
    && echo "install greys by online success." \
    || exit_on_err 1 "install greys by online fail!"


