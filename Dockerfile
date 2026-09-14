FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
RUN chmod +x mvnw
COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /workspace/target/cart-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /app/logs

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
