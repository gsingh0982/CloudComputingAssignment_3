# ---------- stage 1: build ----------
# A Maven image with JDK 21. Nothing is installed on the host: the jar is
# built inside Docker, so a clean machine with only Docker Desktop can run this.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

# Tests are skipped here because the test context would try to reach the
# database, which is not running during the image build.
RUN mvn -B clean package -DskipTests


# ---------- stage 2: run ----------
# Only a JRE, and only the finished jar. Maven and the source code stay
# behind in stage 1, so the final image is much smaller.
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
