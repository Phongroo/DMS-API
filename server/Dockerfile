FROM eclipse-temurin:17-jdk-alpine

VOLUME /tmp

COPY target/dmsserver-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java","-jar","/app.jar"]

#mvn clean package -DskipTest
#docker build -t dmsserver:1.0 .
#kubectl rollout restart deployment dmsserver