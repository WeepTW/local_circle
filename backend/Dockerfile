FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
COPY --chmod=0444 target/local_circle-1.0.0.jar app.jar
USER 10001:10001
EXPOSE 8089
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","/app/app.jar"]
