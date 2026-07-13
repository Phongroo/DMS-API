FROM eclipse-temurin:17-jdk-alpine

VOLUME /tmp

COPY target/dmsserver-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java","--add-opens","java.base/java.util=ALL-UNNAMED","-jar","/app.jar"]

#mvn clean package -DskipTest
#docker build -t dmsserver:1.0 .
# kubectl apply -f dmsserver.yaml
# kubectl apply -f dmsserver-service.yaml
#kubectl rollout restart deployment dmsserver