#!/bin/bash

# program : greys
# author : panjiabang@gmail.com
# date : 2015-04-14

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
	./greys <PID>
	./greys <PID>@[IP]
	./greys <PID>@[IP:PORT]
"
}

main()
{
	typeset local pid=$(echo ${1}|awk -F"@" '{print $1}')
	typeset local ip=$(echo ${1}|awk -F"@|:" '{print $2}')
	typeset local port=$(echo ${1}|awk -F":" '{print $2}')

	# check pid
	if [[ -z $pid ]];then
		echo "illegal arguments, the <PID> must need." >> /dev/stderr
		usage
		exit 1
	fi


	# fix ip
	[[ -z $ip ]]&&ip=${DEFAULT_TARGET_IP}

	# fix port
	[[ -z $port ]]&&port=${DEFAULT_TARGET_PORT}

	# check arg's format
	#typeset local args=${pid}"@"${ip}":"${port}
	#typeset local args_check_flag=$(echo ${args}|awk '/^[0-9]+@([0-9]{1,3}\.){1,3}[0-9]{1,3}:[0-9]+$/')
	#if [[ -z $args_check_flag ]];then
    #    print "illegal arguments." >> /dev/stderr
    #    usage
    #    exit 1
    #fi

	# if env define the JAVA_HOME, use it first
    # if is alibaba opts, use default JAVA_HOME
    [[ -z $JAVA_HOME ]]&&JAVA_HOME=/opt/taobao/java
    typeset local JAVA_VERSION=$(${JAVA_HOME}/bin/java -version 2>&1|awk -F '"' '/java version/&&$2>"1.5"{print $2}')
    if [[ ! -x $JAVA_HOME || -z $JAVA_VERSION ]];then
        print "illegal ENV, please set JAVA_HOME to JDK6+" >> /dev/stderr
        exit 1
    fi

	# fix BOOT_CLASSPATH
    [[ -f $JAVA_HOME/lib/tools.jar ]]&&BOOT_CLASSPATH=-Xbootclasspath/a:$JAVA_HOME/lib/tools.jar

    # fix CHARSET for alibaba opts, we use GBK
    [[ -x /opt/taobao/java ]]&&JVM_OPTS="-Dinput.encoding=GBK"

	typeset local GREYS_ROOT=$(dirname  ${0})
	$JAVA_HOME/bin/java ${BOOT_CLASSPATH} ${JVM_OPTS} -jar ${GREYS_ROOT}/greys.jar -pid ${pid} -target ${ip}":"${port} -multi 0
}

main ${@}