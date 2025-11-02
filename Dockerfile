FROM gradle:8.10.2-jdk21 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
# 로그 자세히 + 스택트레이스 활성화
RUN gradle --no-daemon -i --stacktrace clean bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/app.jar
ENV PORT=8080
EXPOSE 8080
CMD ["java","-Dserver.port=${PORT}","-jar","/app/app.jar"]

