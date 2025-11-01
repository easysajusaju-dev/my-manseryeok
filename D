FROM gradle:8.10.2-jdk21 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN gradle --no-daemon clean bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/app.jar /app/app.jar
ENV PORT=8080
EXPOSE 8080
CMD ["sh","-c","java -Dserver.port=${PORT} -jar /app/app.jar"]
