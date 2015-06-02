#! /bin/bash

GREYS_FILE="greys.zip";

if [ ! -w ./ ]; then
    echo "permission denied, ./ was not writable." >> /dev/stderr
    exit 1;
fi

echo "Downloading... ${GREYS_FILE}";
curl -sLk "http://ompc.oss.aliyuncs.com/greys/greys.zip" -o ${GREYS_FILE};
if [ ! $? -eq 0 ]; then
    echo "download failed!" >> /dev/stderr
    exit 1;
fi

unzip ${GREYS_FILE}
rm -rf ${GREYS_FILE}
chmod +x greys/greys.sh

echo "greys install successed."