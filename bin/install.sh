#! /bin/ksh

typeset GREYS_FILE="greys.zip";

echo "Download... greys.zip";
curl -sLk "http://www.baidupcs.com/file/7f8b82dc12d923b4a73a48b5571d03ec?fid=3039715289-250528-567126668&time=1361341475&sign=FDTA-DCb740ccc5511e5e8fedcff06b081203-vm03Gm1v1CX8dok7TvyBokg%2FJ1s%3D&expires=8h&sh=1&response-cache-control=private" -o $GREYS_FILE;
if [[ ! $? -eq  0 ]]; then
    echo "download file failed!";
    exit -1;
fi 

unzip $GREYS_FILE
rm -rf $GREYS_FILE
chmod +x greys/greys

echo "done. enjoy yourself.^_^"

