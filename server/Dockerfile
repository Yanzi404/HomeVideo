FROM docker.m.daocloud.io/eclipse-temurin:17-jre-alpine

RUN sed -i 's#dl-cdn.alpinelinux.org#mirrors.aliyun.com#g' /etc/apk/repositories \
    && apk add --no-cache ffmpeg

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY homevideo-server-0.1.0.jar app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
