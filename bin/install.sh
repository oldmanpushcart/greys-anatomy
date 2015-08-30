#! /bin/bash

# temp file of greys.sh
TEMP_GREYS_FILE="./greys.sh.$$"

# target file of greys.sh
TARGET_GREYS_FILE="./greys.sh"

# update timeout(sec)
SO_TIMEOUT=60


# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err()
{
    [[ ! -z "${2}" ]] && echo "${2}" 1>&2
    exit ${1}
}

# check permission to download && install
[ ! -w ./ ] && exit_on_err 1 "permission denied, target directory ./ was not writable."

# download from aliyunos
echo "downloading... ${TEMP_GREYS_FILE}";
curl \
    -sLk \
    --connect-timeout ${SO_TIMEOUT} \
    "http://ompc.oss.aliyuncs.com/greys/greys.sh" \
    -o ${TEMP_GREYS_FILE} \
|| exit_on_err 1 "download failed!"

# check download file format
[[ -z $(grep "desc : write for july" ${TEMP_GREYS_FILE}) ]] \
&& exit_on_err 1 "download failed!"

# wirte or overwrite local file
rm -rf greys.sh
mv ${TEMP_GREYS_FILE} ${TARGET_GREYS_FILE}
chmod +x ${TARGET_GREYS_FILE}

# done
echo "greys install successed."