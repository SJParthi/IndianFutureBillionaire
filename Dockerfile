FROM amazoncorretto:17-alpine as builder
WORKDIR /app

COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY gradle gradle
COPY build.gradle build.gradle
COPY src src

RUN chmod +x gradlew
RUN ./gradlew clean bootJar --no-daemon

FROM amazoncorretto:17-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/hft-kite-bot-*.jar /app/hft-kite-bot.jar
EXPOSE 8080
CMD ["java", "-jar", "/app/hft-kite-bot.jar"]
