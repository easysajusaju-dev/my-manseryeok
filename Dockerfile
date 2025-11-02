FROM gradle:8.10.2-jdk21 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .

# 1단계: 먼저 컴파일까지만 해서 "e: ..." 오류 줄을 확실히 노출
RUN gradle --no-daemon --console=plain --stacktrace -i clean compileKotlin -x test

# 2단계: 컴파일 통과하면 jar까지 빌드
RUN gradle --no-daemon --console=plain --stacktrace -i bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/app.jar
ENV PORT=8080
EXPOSE 8080
CMD ["java","-Dserver.port=${PORT}","-jar","/app/app.jar"]
