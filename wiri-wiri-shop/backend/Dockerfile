FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S wiri && adduser -S wiri -G wiri && mkdir -p /data/uploads && chown -R wiri:wiri /data
USER wiri
WORKDIR /app
COPY --from=build /src/target/wiri-wiri-shop-api-*.jar app.jar
ENV STORAGE_DIR=/data/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
