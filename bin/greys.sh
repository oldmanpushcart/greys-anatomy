#!/bin/bash

# program : greysctl
#  author : oldmanpushcart@gmail.com
#    date : 2015-05-04
#    desc : write for july
# version : 1.7.4.0

# define greys's home
GREYS_HOME=${HOME}/.greys

# define greys's lib
GREYS_LIB_DIR=${GREYS_HOME}/lib

# last update greys version
DEFAULT_VERSION="0.0.0.0"

# greys remote url
GREYS_REMOTE_URL="http://ompc.oss.aliyuncs.com/greys"

# update timeout(sec)
SO_TIMEOUT=5

# define default target ip
DEFAULT_TARGET_IP="127.0.0.1"

# define default target port
DEFAULT_TARGET_PORT="3658"

# define JVM's OPS
JVM_OPS="";


# the option to control greys.sh check the permission
OPTION_CHECK_PERMISSION=1

# the option to control greys.sh update version if necessary
OPTION_UPDATE_IF_NECESSARY=1

# the option to control greys.sh attach target jvm
OPTION_ATTACH_JVM=1

# the option to control greys.sh active greys-console
OPTION_ACTIVE_CONSOLE=1

# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err()
{
    [[ ! -z "${2}" ]] && echo "${2}" 1>&2
    exit ${1}
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


# check greys permission
check_permission()
{
    [ ! -w ${HOME} ] \
        && exit_on_err 1 "permission denied, ${HOME} is not writeable."
}


# reset greys work environment
# reset some options for env
reset_for_env()
{

    # init greys' lib
    mkdir -p ${GREYS_LIB_DIR} \
        || exit_on_err 1 "create ${GREYS_LIB_DIR} fail."

    # if env define the JAVA_HOME, use it first
    # if is alibaba opts, use alibaba ops's default JAVA_HOME
    [ -z ${JAVA_HOME} ] && JAVA_HOME=/opt/taobao/java

    # check the jvm version, we need 1.6+
    local JAVA_VERSION=$(${JAVA_HOME}/bin/java -version 2>&1|awk -F '"' '/version/&&$2>"1.5"{print $2}')
    [[ ! -x ${JAVA_HOME} || -z ${JAVA_VERSION} ]] && exit_on_err 1 "illegal ENV, please set \$JAVA_HOME to JDK6+"

    # reset BOOT_CLASSPATH
    [ -f ${JAVA_HOME}/lib/tools.jar ] && BOOT_CLASSPATH=-Xbootclasspath/a:${JAVA_HOME}/lib/tools.jar

    # reset CHARSET for alibaba opts, we use GBK
    [[ -x /opt/taobao/java ]]&& JVM_OPTS="${JVM_OPTS} -Dinput.encoding=GBK"

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

# get latest version from remote
get_remote_version()
{
    curl -sLk --connect-timeout ${SO_TIMEOUT} "${GREYS_REMOTE_URL}/version"\
        |awk -F "." '{printf("%03d.%03d.%03d.%03d\n",$1,$2,$3,$4)}'\
        |awk '/^[0-9][0-9][0-9].[0-9][0-9][0-9].[0-9][0-9][0-9].[0-9][0-9][0-9]$/'\
        |awk -F "." '{printf("%d.%d.%d.%d\n",$1,$2,$3,$4)}'

}

# make version format to comparable format like 000.000.000.000
# $1 : version
to_comparable_version()
{
    echo ${1}|awk -F "." '{printf("%d.%d.%d.%d\n",$1,$2,$3,$4)}'
}

# update greys if necessary
update_if_necessary()
{

    # get local update version
    local local_version=$(default $(get_local_version) ${DEFAULT_VERSION})

    # get remove update version
    local remote_version=$(default $(get_remote_version) ${DEFAULT_VERSION})

    # update_if_necessary
    if [[ $(to_comparable_version ${local_version}) < $(to_comparable_version ${remote_version}) ]]; then

        echo "new version(${remote_version}) detection, update now..."

        local temp_target_lib_dir="${GREYS_LIB_DIR}/temp_${remote_version}_$$"
        local temp_target_lib_zip="${temp_target_lib_dir}/greys-${remote_version}-bin.zip"
        local target_lib_dir="${GREYS_LIB_DIR}/${remote_version}"

        # clean
        rm -rf ${temp_target_lib_dir}
        rm -rf ${target_lib_dir}

        mkdir -p "${temp_target_lib_dir}" \
            || exit_on_err 1 "create ${temp_target_lib_dir} fail."

        # download current greys version
        curl \
            -#Lk \
            --connect-timeout ${SO_TIMEOUT} \
            -o ${temp_target_lib_zip} \
            "${GREYS_REMOTE_URL}/release/greys-${remote_version}-bin.zip" \
        || return 1

        # unzip greys lib
        unzip ${temp_target_lib_zip} -d ${temp_target_lib_dir} || return 1

        # rename
        mv ${temp_target_lib_dir} ${target_lib_dir} || return 1

        # print success
        echo "update completed."

    fi

}

# the usage
usage()
{
    echo "
greys usage:
    the format was <PID>[@IP:PORT]
     <PID> : the target Java Process ID
      [IP] : the target's IP
    [PORT] : the target's PORT

example:
    ./greys.sh <PID>
    ./greys.sh <PID>@[IP]
    ./greys.sh <PID>@[IP:PORT]
"
}

# parse the argument
parse_arguments()
{

     TARGET_PID=$(echo ${1}|awk -F "@"   '{print $1}');
      TARGET_IP=$(echo ${1}|awk -F "@|:" '{print $2}');
    TARGET_PORT=$(echo ${1}|awk -F ":"   '{print $2}');

    # check pid
    if [ -z ${TARGET_PID} ];then
        echo "illegal arguments, the <PID> is required." 1>&2
        return 1
    fi

    # reset ${ip} to default if empty
    [ -z ${TARGET_IP} ] && TARGET_IP=${DEFAULT_TARGET_IP}

    # reset ${port} to default if empty
    [ -z ${TARGET_PORT} ] && TARGET_PORT=${DEFAULT_TARGET_PORT}

    return 0

}


# attach greys to target jvm
# $1 : greys_local_version
attach_jvm()
{
    local greys_lib_dir=${GREYS_LIB_DIR}/${1}/greys

    if [ ${TARGET_IP} = ${DEFAULT_TARGET_IP} ]; then
        ${JAVA_HOME}/bin/java \
            ${BOOT_CLASSPATH} ${JVM_OPTS} \
            -jar ${greys_lib_dir}/greys-core.jar \
                -pid ${TARGET_PID} \
                -target ${TARGET_IP}":"${TARGET_PORT} \
                -core "${greys_lib_dir}/greys-core.jar" \
                -agent "${greys_lib_dir}/greys-agent.jar"
    fi
}

# active console
# $1 : greys_local_version
active_console()
{

    local greys_lib_dir=${GREYS_LIB_DIR}/${1}/greys

    if type ${JAVA_HOME}/bin/java 2>&1 >> /dev/null; then

        # use default console
        ${JAVA_HOME}/bin/java \
            -cp ${greys_lib_dir}/greys-core.jar \
            com.github.ompc.greys.core.GreysConsole \
                ${TARGET_IP} \
                ${TARGET_PORT}

    elif type telnet 2>&1 >> /dev/null; then

        # use telnet
        telnet ${TARGET_IP} ${TARGET_PORT}

    elif type nc 2>&1 >> /dev/null; then

        # use netcat
        nc ${TARGET_IP} ${TARGET_PORT}

    else

        echo "'telnet' or 'nc' is required." 1>&2
        return 1

    fi
}

# the main
main()
{

    while getopts "PUJC" ARG
    do
        case ${ARG} in
            P) OPTION_CHECK_PERMISSION=0;;
            U) OPTION_UPDATE_IF_NECESSARY=0;;
            J) OPTION_ATTACH_JVM=0;;
            C) OPTION_ACTIVE_CONSOLE=0;;
            ?) usage;exit 1;;
        esac
    done

    shift $((OPTIND-1));

    if [[ ${OPTION_CHECK_PERMISSION} -eq 1 ]]; then
        check_permission
    fi

    reset_for_env

    parse_arguments "${@}" \
        || exit_on_err 1 "$(usage)"

    if [[ ${OPTION_UPDATE_IF_NECESSARY} -eq 1 ]]; then
        update_if_necessary \
            || echo "update fail, ignore this update." 1>&2
    fi

    local greys_local_version=$(default $(get_local_version) ${DEFAULT_VERSION})

    if [[ ${greys_local_version} = ${DEFAULT_VERSION} ]]; then
        exit_on_err 1 "greys not found, please check your network."
    fi

    if [[ ${OPTION_ATTACH_JVM} -eq 1 ]]; then
        attach_jvm ${greys_local_version}\
            || exit_on_err 1 "attach to target jvm(${TARGET_PID}) failed."
    fi

    if [[ ${OPTION_ACTIVE_CONSOLE} -eq 1 ]]; then
        active_console ${greys_local_version}\
            || exit_on_err 1 "active console failed."
    fi

}


main "${@}"
