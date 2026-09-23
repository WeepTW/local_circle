FROM eclipse-temurin:21-jre-alpine@sha256:1a29e1fe337eb28b5bec30f0ee8ed29f0ff80ab6f75dcf9313efe82911065a52
RUN apk del --no-cache gnupg coreutils && apk upgrade --no-cache
WORKDIR /app
COPY --chmod=0444 target/local_circle-1.0.0.jar app.jar
USER 10001:10001
EXPOSE 8089
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","/app/app.jar"]
