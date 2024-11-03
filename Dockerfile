FROM --platform=linux/amd64 openjdk:11
COPY target/enhance-ai-platform*.jar /usr/src/enhance-ai-platform.jar
COPY src/main/resources/application.properties /opt/conf/application.properties
CMD ["java", "-jar", "/usr/src/enhance-ai-platform.jar", "--spring.config.location=file:/opt/conf/application.properties"]

