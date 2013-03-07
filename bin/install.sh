#! /bin/ksh

typeset GREYS_FILE="greys.zip";

echo "Download... greys.zip";
curl -sLk "http://greys.oss.aliyuncs.com/greys.zip?file=greys.zip" -o $GREYS_FILE;
if [[ ! $? -eq  0 ]]; then
    echo "download file failed!";
    exit -1;
fi 

unzip $GREYS_FILE
rm -rf $GREYS_FILE
chmod +x greys/greys

echo "done. enjoy yourself.^_^"

