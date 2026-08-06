FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw --batch-mode dependency:go-offline

COPY src src
RUN ./mvnw --batch-mode package

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build --chown=1001:1001 /workspace/target/liminal-chat-*.jar application.jar

USER 1001:1001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "application.jar"]
