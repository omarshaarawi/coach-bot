FROM eclipse-temurin:18
ADD build/distributions/coach-bot.tar /
EXPOSE 8080
ENTRYPOINT ["/coach-bot/bin/coach-bot"]
