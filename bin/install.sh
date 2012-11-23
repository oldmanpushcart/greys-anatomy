#! /bin/ksh

typeset downloads=$(curl -sLk https://api.github.com/repos/oldmanpushcart/greys-anatomy/downloads);
typeset CHECKSUM=$(echo "$downloads" |awk -F "\"" '/\"description\":/{print $4}'|head -1);
typeset URL=$(echo "$downloads" |awk -F "\"" '/\"html_url\":/{print $4}'|head -1);
typeset OS=$(uname);
typeset GREYS_FILE="greys.zip";

echo "Download... greys.zip";
curl -sLk $URL > $GREYS_FILE;
if [[ ! $? -eq  0 ]]; then
    echo "download file failed!";
    exit -1;
fi 



# check the checksum
if [[ -z $CHECKSUM ]]; then
    echo "Warning, the checksum is not found at description.";
else 
    typeset md5;
    if [[ $OS = "Darwin" ]]; then
        md5=$(cat $GREYS_FILE | md5 | awk '{print $1}');
    elif [[ $os = "Linux" ]]; then
        md5=$(cat $GREYS_FILE | md5sum | awk '{print $1}');
    fi
    if [[ $md5 != $CHECKSUM  ]]; then
        echo "checksum failed!, please check your download file.";
        exit -1;
    else
        echo "checksum successed."
    fi 
fi

unzip greys.zip
rm -rf greys.zip
chmod +x greys/greys

echo "done. enjoy yourself."

