FROM openjdk:17.0.2-jdk

WORKDIR /app

COPY gradlew /app/gradlew
COPY gradle /app/gradle
COPY build.gradle /app/
COPY settings.gradle /app/
COPY src /app/src

RUN chmod +x ./gradlew

RUN ./gradlew shadowJar

RUN mv /app/build/libs/DisKube.jar /app/DisKube.jar && rm -rf /app/build /app/src /app/gradle /app/*.gradle /app/gradlew

CMD ["sh", "-c", "java -jar DisKube.jar ${DISCORD_BOT_TOKEN} ${DISCORD_WEBHOOK}"]
