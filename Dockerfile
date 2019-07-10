FROM openjdk:8-jre-alpine

COPY target/soundcloudbot.jar /soundcloudbot.jar

CMD ["/usr/bin/java", "-jar", "/soundcloudbot.jar"]
