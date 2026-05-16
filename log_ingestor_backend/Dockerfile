FROM eclipse-temurin:11-jre

WORKDIR /app

COPY target/universal/stage .

EXPOSE 3000

CMD ["bin/log_ingestor_backend"]
