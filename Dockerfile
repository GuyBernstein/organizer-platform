FROM --platform=linux/amd64 openjdk:11
COPY target/orginizer-platform*.jar /usr/src/orginizer-platform.jar
COPY src/main/resources/application.properties /opt/conf/application.properties
CMD ["java", "-jar", "/usr/src/orginizer-platform.jar", "--spring.config.location=file:/opt/conf/application.properties"]

