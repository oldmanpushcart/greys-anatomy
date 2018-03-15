#!/usr/bin/env bash

# program : greys
#  author : oldmanpushcart@gmail.com
#    date : 2018-03-12
#    desc : write for july
# version : 2.0.0.0

# enable auto update greys
ENABLE_AUTO_UPDATE=1

# define greys's base dir
GREYS_BASE_DIR=${HOME}/.greys2

# define network timeout
SO_TIMEOUT_SEC=60

# the latest version in local
LATEST_LOCAL_VERSION=$(
    ls ${GREYS_BASE_DIR} 2>/dev/null \
        |grep -E '^([0-9]+\.){3}[0-9]+$' \
        |sort \
        |tail -1
);

LATEST_REMOTE_VERSION=$(
    [[ ${ENABLE_AUTO_UPDATE} ]] \
        && curl -sLk --connect-timeout ${SO_TIMEOUT_SEC} \
                "http://ompc.oss.aliyuncs.com/greys/version" \
           | grep -E '^([0-9]+\.){3}[0-9]+$'
);

# define current version
CURRENT_VERSION=${LATEST_LOCAL_VERSION}


# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err()
{
    [[ ! -z "${2}" ]] && echo "${2}" 1>&2
    exit ${1}
}

# the usage
usage()
{
    echo "
USAGE:
    the format was <PID>[@IP:PORT]
     <PID> : the target Java Process ID
      [IP] : the target's IP
    [PORT] : the target's PORT

EXAMPLE:
    ./greys.sh <PID>
    ./greys.sh <PID>@[IP:PORT]
"
}


# update greys if necessary
# maybe change ${CURRENT_VERSION}
function update_if_necessary()
{
    # check local version is less then remote version
    [[ ! ${LATEST_REMOTE_VERSION} || ${LATEST_REMOTE_VERSION} < ${LATEST_LOCAL_VERSION} ]] \
        && return

    echo "found update from server, latest version:${LATEST_REMOTE_VERSION}"

    # download & install

}

# get execute java command
# $1 : target java PID
function get_java_bin()
{
    local t_pid=${1}
    [[ -z ${JAVA_HOME} ]] \
        && echo "${JAVA_HOME}/bin/java" \
        || ps u \
            |awk '$2=='${t_pid}'{print $11}'\
            |grep -E "java$"
}

# get the JRE version
# $1 : java_bin
function get_jre_version() {
    local java_bin=${1}
    ${java_bin} -version 2>&1|awk -F '"' '/version/{print $2}'
}

function main()
{

    local t_pid;        # the target java process ID
    local t_add;        # the target greys server address
    local java_bin;     # the executable java bin
    local sandbox_sh;   # the sandbox shell
    local sandbox_addr; # the sandbox ip
    local sandbox_port; # the sandbox port

    # check pid
    t_pid=${@%@*}
    [[ ! ${t_pid} =~ [0-9]+ ]] \
        && exit_on_err 1 "<PID> was required. $(usage)"

    # check address
    t_add=${@#*@}
    [[ -z ${t_add} && ! ${t_add} =~ ^[^:]+:[0-9]+$ ]] \
        && exit_on_err 1 "illegal [@IP:PORT] $(usage)"

    # check JRE
    java_bin=$(get_java_bin ${t_pid})
    [[ ${java_bin} ]] \
        || exit_on_err 1 "JRE not found." \
        && [[ $(get_jre_version ${java_bin}) > "1.5" ]] \
        || exit_on_err 1 "JRE version must large then JRE6+, current is $(get_jre_version ${java_bin})"

    # check ${CURRENT_VERSION}
    update_if_necessary || test 1 \
        && [[ ${CURRENT_VERSION} ]] \
        || exit_on_err 1 "greys not installed."

    # bootstrap JVM-SANDBOX & greys-module
    sandbox_sh=${GREYS_BASE_DIR}/${CURRENT_VERSION}/greys/sandbox/bin/sandbox.sh
    [[ $(${sandbox_sh} -p ${t_pid} -n greys -l|awk '$1=="greys"{print $1}') == "greys" ]] \
        || exit_on_err 1 "bootstrap greys fail." \
        && sandbox_addr=$(${sandbox_sh} -p ${t_pid} -n greys -v|sed 's/ //g'|awk -F: '$1=="SERVER_ADDR"{print $2}') \
        && sandbox_port=$(${sandbox_sh} -p ${t_pid} -n greys -v|sed 's/ //g'|awk -F: '$1=="SERVER_PORT"{print $2}')

    # show banner
    ${sandbox_sh} -p ${t_pid} -n greys -d "greys/banner?pid=${t_pid}" \

    # bootstrap console
    ${java_bin} -jar ${GREYS_BASE_DIR}/${CURRENT_VERSION}/greys/greys-console.jar \
            --ip=${sandbox_addr} \
            --port=${sandbox_port} \
            --namespace="greys" \
    || exit_on_err 1 "bootstrap console fail."

}


main "${@}"



