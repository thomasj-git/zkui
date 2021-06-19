#!/usr/bin/env bash

imgName=zkui
imgVer=latest
imgTag=${imgName}:${imgVer}

cIds=`docker ps -a \
|grep ${imgName} \
|grep ${imgVer} \
|awk '{print $1}'`

if [[ -n "${cIds}" ]]; then
docker rm -f ${cIds}
fi

imgIds=`docker images \
|grep "${imgName}" \
|grep "${imgVer}" \
|awk '{print $3}'`

if [[ -n "${imgIds}" ]]; then
docker rmi -f ${imgIds}
fi

rm -rf lib
cp -rf ../lib .
cp ../config/config.cfg .

docker build -t ${imgTag} .