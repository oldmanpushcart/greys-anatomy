#!/bin/bash

# program : greys
#  author : panjiabang@gmail.com
#  author : oldmanpushcart@gmail.com
#    date : 2014-10-23
#    desc : rewrite for july by vlinux
# version : 1.5.4.8

DEFAULT_TARGET_IP="127.0.0.1"
DEFAULT_TARGET_PORT="3658"

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

pid=$(echo ${1}|awk -F"@" '{print $1}');
ip=$(echo ${1}|awk -F"@|:" '{print $2}');
port=$(echo ${1}|awk -F":" '{print $2}');

# define greys's home
GREYS_ROOT=$(dirname  ${0})

# define JVM's OPS
JVM_OPS="";

# check pid
if [[ -z ${pid} ]];then
    echo "illegal arguments, the <PID> must need." >> /dev/stderr
    usage
    exit 1
fi

# reset ${ip} to default if empty
[[ -z ${ip} ]]&& ip=${DEFAULT_TARGET_IP}

# reset ${port} to default if empty
[[ -z ${port} ]]&& port=${DEFAULT_TARGET_PORT}

# if env define the JAVA_HOME, use it first
# if is alibaba opts, use alibaba ops's default JAVA_HOME
[[ -z ${JAVA_HOME} ]]&& JAVA_HOME=/opt/taobao/java
typeset local JAVA_VERSION=$(${JAVA_HOME}/bin/java -version 2>&1|awk -F '"' '/java version/&&$2>"1.5"{print $2}')
if [[ ! -x ${JAVA_HOME} || -z ${JAVA_VERSION} ]];then
    print "illegal ENV, please set JAVA_HOME to JDK6+" >> /dev/stderr
    exit 1
fi

# fix BOOT_CLASSPATH
[[ -f ${JAVA_HOME}/lib/tools.jar ]]&& BOOT_CLASSPATH=-Xbootclasspath/a:${JAVA_HOME}/lib/tools.jar

# fix CHARSET for alibaba opts, we use GBK
[[ -x /opt/taobao/java ]]&& JVM_OPTS="${JVM_OPTS} -Dinput.encoding=GBK"

${JAVA_HOME}/bin/java ${BOOT_CLASSPATH} ${JVM_OPTS} -jar ${GREYS_ROOT}/greys.jar -pid ${pid} -target ${ip}":"${port} -multi 0
