#!/bin/bash

# program : greys-attach
#  author : oldmanpushcart@gmail.com
#    date : 2016-02-05
#    desc : write for july
# version : 1.7.4.0

# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err()
{
    [[ ! -z "${2}" ]] && echo "${2}" 1>&2
    exit ${1}
}


OPTION_SILENT=0

while getopts "s" ARG
do
    case ${ARG} in
        s) OPTION_SILENT=1;;
    esac
done

shift $((OPTIND-1));

if [[ ${OPTION_SILENT} -eq 1 ]]; then
    ./greys.sh -C "${@}" || exit 1
else
    ./greys.sh -C "${@}" \
        && echo "greys attach to target(${@}) success" \
        || exit_on_err 1 "greys attach to target(${@}) failed."
fi

