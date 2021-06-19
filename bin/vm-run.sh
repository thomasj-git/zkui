#!/usr/bin/env bash

APP_SN=zkui

PSN=`ps -ef|grep ${APP_SN}|grep -v grep|awk '{print $2}'`
if [[ -n "${PSN}" ]]; then
echo "killed ${PSN}"
kill -9 ${PSN}
fi

CLASSPATH=.
for i in `ls ../lib/*.jar`;
do
CLASSPATH=$CLASSPATH:$i
done

cp ../config/config.cfg .

nohup java -classpath $CLASSPATH com.deem.zkui.Main &