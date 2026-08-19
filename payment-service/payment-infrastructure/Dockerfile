FROM eclipse-temurin:21-jre-alpine

# adduser, not useradd: busybox on Alpine ships no useradd.
RUN adduser --system --disabled-password --no-create-home airline

WORKDIR /app
COPY target/payment-infrastructure-0.0.1-SNAPSHOT.jar app.jar

USER airline
EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
