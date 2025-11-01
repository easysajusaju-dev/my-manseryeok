FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew && ./gradlew --no-daemon clean bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar /app/app.jar
ENV PORT=8080
EXPOSE 8080
CMD ["java","-Dserver.port=${PORT}","-jar","/app/app.jar"]

