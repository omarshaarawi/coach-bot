FROM eclipse-temurin:18
ADD build/distributions/coach-bot.tar /
EXPOSE 8080 8081
ENTRYPOINT ["/coach-bot/bin/coach-bot"]
