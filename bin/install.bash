#! /bin/bash

GREYS_FILE="greys.zip";

echo "Download... greys.zip";
curl -sLk "http://ompc.oss-cn-hangzhou.aliyuncs.com/greys/greys.zip" -o $GREYS_FILE;
if [[ ! $? -eq  0 ]]; then
    echo "download file failed!";
    exit -1;
fi

unzip $GREYS_FILE
rm -rf $GREYS_FILE
chmod +x greys/greys

echo "done. enjoy yourself.^_^"