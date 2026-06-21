FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy only Maven files first
COPY pom.xml .

# Download dependencies in a cacheable layer
RUN mvn dependency:go-offline -DskipTests

# Now copy source code
COPY src ./src

# Build app
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system spring && useradd --system --gid spring --create-home spring

USER spring:spring
WORKDIR /app

COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 80

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
