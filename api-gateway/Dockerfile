# Two stages: the first builds, the second runs. Nothing but the jar crosses,
# so the image that ships has no Maven, no sources and no build cache in it.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# The pom first, on its own layer, so dependencies are fetched once and reused
# until it changes.
COPY pom.xml ./
RUN mvn -B dependency:go-offline -DskipTests

COPY . .
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine

# adduser, not useradd: busybox on Alpine ships no useradd.
RUN adduser --system --disabled-password --no-create-home airline

WORKDIR /app
COPY --from=build /build/target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

USER airline
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
