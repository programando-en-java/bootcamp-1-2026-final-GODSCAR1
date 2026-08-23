# Two stages: the first builds, the second runs. Nothing but the jar crosses,
# so the image that ships has no Maven, no sources and no build cache in it.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# The poms first, on their own layer. Dependencies are fetched once and reused
# until a pom changes, so editing Java does not download the internet again.
COPY pom.xml ./
COPY notification-domain/pom.xml notification-domain/
COPY notification-application/pom.xml notification-application/
COPY notification-infrastructure/pom.xml notification-infrastructure/
RUN mvn -B dependency:go-offline -DskipTests

COPY . .
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine

# adduser, not useradd: busybox on Alpine ships no useradd.
RUN adduser --system --disabled-password --no-create-home airline

WORKDIR /app
COPY --from=build /build/notification-infrastructure/target/notification-infrastructure-0.0.1-SNAPSHOT.jar app.jar

USER airline
EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
