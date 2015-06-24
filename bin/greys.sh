#!/bin/bash

# program : greys
#  author : oldmanpushcart@gmail.com
#    date : 2015-05-04
#    desc : write for july
# version : 1.6.0.0

# define default target ip
DEFAULT_TARGET_IP="127.0.0.1"

# define default target port
DEFAULT_TARGET_PORT="3658"

# define greys's home
GREYS_ROOT=$(dirname ${0})

# define JVM's OPS
JVM_OPS="";


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
        echo "illegal arguments, the <PID> is required." >> /dev/stderr
        return 1
    fi

    # reset ${ip} to default if empty
    [ -z ${TARGET_IP} ] && TARGET_IP=${DEFAULT_TARGET_IP}

    # reset ${port} to default if empty
    [ -z ${TARGET_PORT} ] && TARGET_PORT=${DEFAULT_TARGET_PORT}

    return 0

}

# reset some options for env
reset_for_env()
{

    # if env define the JAVA_HOME, use it first
    # if is alibaba opts, use alibaba ops's default JAVA_HOME
    [ -z ${JAVA_HOME} ] && JAVA_HOME=/opt/taobao/java

    # check the jvm version, we need 1.6+
    local JAVA_VERSION=$(${JAVA_HOME}/bin/java -version 2>&1|awk -F '"' '/java version/&&$2>"1.5"{print $2}')
    if [[ ! -x ${JAVA_HOME} || -z ${JAVA_VERSION} ]];then
        echo "illegal ENV, please set \$JAVA_HOME to JDK6+" >> /dev/stderr
        exit 1
    fi

    # fix BOOT_CLASSPATH
    [ -f ${JAVA_HOME}/lib/tools.jar ] && BOOT_CLASSPATH=-Xbootclasspath/a:${JAVA_HOME}/lib/tools.jar

    # fix CHARSET for alibaba opts, we use GBK
    [[ -x /opt/taobao/java ]]&& JVM_OPTS="${JVM_OPTS} -Dinput.encoding=GBK"

}

# attach greys to target jvm
attach_jvm()
{
    ${JAVA_HOME}/bin/java \
        ${BOOT_CLASSPATH} \
        ${JVM_OPTS} \
        -jar ${GREYS_ROOT}/greys.jar -pid ${TARGET_PID} -target ${TARGET_IP}":"${TARGET_PORT}
}

# active console
active_console()
{

    if type ${JAVA_HOME}/bin/java 2>&1 >> /dev/null; then

        # use default console
        ${JAVA_HOME}/bin/java -cp ${GREYS_ROOT}/greys.jar com.github.ompc.greys.GreysConsole ${TARGET_IP} ${TARGET_PORT}

    elif type telnet 2>&1 >> /dev/null; then

        # use telnet
        telnet ${TARGET_IP} ${TARGET_PORT}

    elif type nc 2>&1 >> /dev/null; then

        # use netcat
        nc ${TARGET_IP} ${TARGET_PORT}

    else

        echo "'telnet' or 'nc' is required." >> /dev/stderr
        return 1

    fi
}


parse_arguments $@
if [ ! $? -eq 0 ]; then
    usage
    exit 1
fi


reset_for_env

attach_jvm
if [ ! $? -eq 0 ];then
    echo "attach to target jvm(${TARGET_PID}) failed." >> /dev/stderr
    exit 1
fi

active_console



