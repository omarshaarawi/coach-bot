FROM eclipse-temurin:18
ADD build/distributions/coach-bot.tar /
ENTRYPOINT ["/coach-bot/bin/coach-bot"]