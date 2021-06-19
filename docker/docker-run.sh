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

docker run -d --name zkui -p 9090:9090 --restart=always zkui:latest
