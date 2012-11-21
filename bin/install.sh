#!/bin/bash
curl -sLk http://greys-anatomy.googlecode.com/files/ga.zip > ga.zip || {
    echo "download greys-anatomy failed. please try it later.";
    rm -f ga.zip
    rm -rf ga
    exit -1;
}

echo "download greys-anatomy successed."
unzip ga.zip
chmod +x ga/ga
