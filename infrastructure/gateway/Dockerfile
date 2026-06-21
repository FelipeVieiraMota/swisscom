FROM maven:3.9.9-eclipse-temurin-21 AS build

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY pom.xml /app/
COPY src /app/src
WORKDIR /app

RUN mvn clean package -DskipTests \
    && jar tf /app/target/gateway-0.0.1-SNAPSHOT.jar >/dev/null

FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system spring && useradd --system --gid spring --create-home spring

USER spring:spring
WORKDIR /app

COPY --from=build /app/target/gateway-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 80

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
