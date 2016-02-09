#!/bin/bash

# program : greys-shell
#  author : oldmanpushcart@gmail.com
#    date : 2016-02-05
#    desc : write for july
# version : 1.7.4.0

# define greys's home
GREYS_HOME=${HOME}/.greys

# define greys's lib
GREYS_LIB_DIR=${GREYS_HOME}/lib

# last update greys version
DEFAULT_VERSION="0.0.0.0"

DEFAULT_TARGET_IP=127.0.0.1
DEFAULT_TARGET_PORT=3658

TARGET_IP=${DEFAULT_TARGET_IP}
TARGET_PORT=${DEFAULT_TARGET_PORT}

# reset greys work environment
# reset some options for env
reset_for_env()
{

    # if env define the JAVA_HOME, use it first
    # if is alibaba opts, use alibaba ops's default JAVA_HOME
    [ -z ${JAVA_HOME} ] && JAVA_HOME=/opt/taobao/java

    # check the jvm version, we need 1.6+
    local JAVA_VERSION=$(${JAVA_HOME}/bin/java -version 2>&1|awk -F '"' '/version/&&$2>"1.5"{print $2}')
    [[ ! -x ${JAVA_HOME} || -z ${JAVA_VERSION} ]] && exit_on_err 1 "illegal ENV, please set \$JAVA_HOME to JDK6+"

}

# parse the argument
parse_arguments()
{

      TARGET_IP=$(echo ${1}|awk -F ":" '{print $1}');
    TARGET_PORT=$(echo ${1}|awk -F ":" '{print $2}');

    # reset ${ip} to default if empty
    [ -z ${TARGET_IP} ] && TARGET_IP=${DEFAULT_TARGET_IP}

    # reset ${port} to default if empty
    [ -z ${TARGET_PORT} ] && TARGET_PORT=${DEFAULT_TARGET_PORT}

    return 0

}


# get latest version from local
get_local_version()
{
    ls ${GREYS_LIB_DIR}\
        |awk -F "." '{printf("%03d.%03d.%03d.%03d\n",$1,$2,$3,$4)}'\
        |sort\
        |awk '/^[0-9][0-9][0-9].[0-9][0-9][0-9].[0-9][0-9][0-9].[0-9][0-9][0-9]$/'\
        |tail -1\
        |awk -F "." '{printf("%d.%d.%d.%d\n",$1,$2,$3,$4)}'
}

# get with default value
# $1 : target value
# $2 : default value
default()
{
    [[ ! -z "${1}" ]] \
        && echo "${1}" \
        || echo "${2}"
}

# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err()
{
    [[ ! -z "${2}" ]] && echo "${2}" 1>&2
    exit ${1}
}


# greys netcat
greys_nc()
{
    local greys_lib_dir=${GREYS_LIB_DIR}/${1}/greys
    while read line;
    do
        echo ${line} \
            | ${JAVA_HOME}/bin/java \
                -cp ${greys_lib_dir}/greys-core.jar \
                com.github.ompc.greys.core.util.NetCat \
                    ${TARGET_IP} \
                    ${TARGET_PORT}
    done
}

# the usage
usage()
{
    echo "
greys shell usage:
    the format was [@IP:PORT]
      [IP] : the target's IP, default ${DEFAULT_TARGET_IP}
    [PORT] : the target's PORT, default ${DEFAULT_TARGET_PORT}

example:
    echo help|./gs.sh [IP]
    echo help|./gs.sh [IP:PORT]
"
}

main()
{

    while getopts "h" ARG
    do
        case ${ARG} in
            h) usage;exit 0;;
            *) usage;exit 1;;
        esac
    done

    reset_for_env

    parse_arguments "${@}" \
        || exit_on_err 1 "$(usage)"

    local greys_local_version=$(default $(get_local_version) ${DEFAULT_VERSION})
    greys_nc ${greys_local_version}

}

main "${@}"