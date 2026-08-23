# Two stages: the first builds, the second runs. Nothing but the jar crosses,
# so the image that ships has no Maven, no sources and no build cache in it.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# The poms first, on their own layer. Dependencies are fetched once and reused
# until a pom changes, so editing Java does not download the internet again.
COPY pom.xml ./
COPY booking-domain/pom.xml booking-domain/
COPY booking-application/pom.xml booking-application/
COPY booking-infrastructure/pom.xml booking-infrastructure/
RUN mvn -B dependency:go-offline -DskipTests

COPY . .
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine

# adduser, not useradd: busybox on Alpine ships no useradd.
RUN adduser --system --disabled-password --no-create-home airline

WORKDIR /app
COPY --from=build /build/booking-infrastructure/target/booking-infrastructure-0.0.1-SNAPSHOT.jar app.jar

USER airline
EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
