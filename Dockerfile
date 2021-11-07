FROM openjdk:16-jdk-alpine
ADD build/libs/coach-bot-1.0.0.jar coach-bot.jar
EXPOSE 9000 9001 9002
ENTRYPOINT ["java", "-jar", "coach-bot.jar"]