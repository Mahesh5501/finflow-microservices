# FinFlow identity-service — multi-stage build (JDK 21 + Maven → JRE 21)
# Build:  docker build -t finflow/identity-service .
# Run:    docker run --rm -p 18081:18081 finflow/identity-service

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --system --gid 10001 finflow \
 && useradd  --system --uid 10001 --gid finflow --home-dir /app --shell /usr/sbin/nologin finflow

COPY --from=build /workspace/target/identity-service-*.jar /app/app.jar
RUN chown finflow:finflow /app/app.jar

USER finflow
EXPOSE 18081

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
