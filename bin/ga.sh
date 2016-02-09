#!/bin/bash

# program : greys-attach
#  author : oldmanpushcart@gmail.com
#    date : 2016-02-05
#    desc : write for july
# version : 1.7.4.0


# the usage
usage()
{
    echo "
greys attach usage:
    the format was [-s] <PID>
     <PID> : the target Java Process ID
        -s : attach jvm with silent

example:
    ./ga.sh <PID>
    ./ga.sh -s <PID>
"
}

# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err()
{
    [[ ! -z "${2}" ]] && echo "${2}" 1>&2
    exit ${1}
}


# the option to control ga.sh attach jvm with silent
OPTION_SILENT=0

while getopts "sh" ARG
do
    case ${ARG} in
        s) OPTION_SILENT=1;;
        h) usage;exit 0;;
        *) usage;exit 1;;
    esac
done

shift $((OPTIND-1));

if [[ $# -lt 1 ]]; then
    exit_on_err 1 "illegal arguments, the <PID> is required."
fi

if [[ ${OPTION_SILENT} -eq 1 ]]; then
    ./greys.sh -C "${@}" || exit 1
else
    ./greys.sh -C "${@}" \
        && echo "greys attach to target(${@}) success" \
        || exit_on_err 1 "greys attach to target(${@}) failed."
fi

