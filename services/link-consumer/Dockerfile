FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -DskipTests
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system spring && useradd --system --gid spring --create-home spring

USER spring:spring
WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 5100
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
