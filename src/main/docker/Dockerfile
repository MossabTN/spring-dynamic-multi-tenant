####
# This Dockerfile is used in order to build a container that runs the Spring application
#
# Before building the docker image run:
#
# mvn package
#
# Then, build the image with:
#
# docker build -f src/main/docker/Dockerfile -t maxilog/spring-multi-tenant-jvm .
#
# Then run the container using:
#
# docker run -i --rm -p 8080:8080 maxilog/spring-multi-tenant-jvm
#
###
FROM openjdk:8-jdk-alpine
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]