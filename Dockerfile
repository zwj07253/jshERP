# Stage 1: Build backend
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY jshERP-boot/pom.xml ./pom.xml
RUN mvn dependency:go-offline -B
COPY jshERP-boot/src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/target/jshERP.jar ./jshERP.jar
RUN mkdir -p /opt/jshERP/upload /opt/jshERP/export /opt/jshERP/plugins /opt/tmp/tomcat
EXPOSE 9999
ENTRYPOINT ["java", "-jar", "jshERP.jar"]
