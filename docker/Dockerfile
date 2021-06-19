FROM java:8

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /

ENV LANG C.UTF-8

ENTRYPOINT ["/bin/bash", "/bootstrap.sh"]

COPY / .

RUN chmod +x /bootstrap.sh
