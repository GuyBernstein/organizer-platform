FROM  openjdk:11
# --platform=linux/amd64
COPY target/organizer-platform*.jar /usr/src/organizer-platform.jar
COPY src/main/resources/application.properties /opt/conf/application.properties
CMD ["java", "-jar", "/usr/src/organizer-platform.jar", "--spring.config.location=file:/opt/conf/application.properties"]

