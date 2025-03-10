FROM maven:3.8.7-openjdk-18-slim
WORKDIR /app
COPY src /app/src
COPY pom.xml /app
RUN mvn clean package -DskipTests
CMD ["java", "-jar", "target/c2c-mvt.jar"]