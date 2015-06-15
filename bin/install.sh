#! /bin/bash

# temp greys file
TEMP_GREYS_FILE="./greys.zip.$$"

# check permission to download && install
if [ ! -w ./ ]; then
    echo "permission denied, target directory is not writable." >> /dev/stderr
    exit 1
fi



# download from aliyunos
echo "downloading... ${TEMP_GREYS_FILE}";
curl -sLk "http://ompc.oss.aliyuncs.com/greys/greys.zip" -o ${TEMP_GREYS_FILE};
if [ ! $? -eq 0 ]; then
    echo "download failed!" >> /dev/stderr
    exit 1
fi


# clean if exists
rm -rf ./greys

# unzip the greys file
unzip ${TEMP_GREYS_FILE}
if [ ! $? -eq 0 ]; then
    echo "file damage!" >> /dev/stderr
    exit 1
fi

# install greys
rm -rf ${TEMP_GREYS_FILE}
chmod +x greys/greys.sh

# done
echo "greys install successed."