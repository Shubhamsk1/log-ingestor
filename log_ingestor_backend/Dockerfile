FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/universal/stage .

EXPOSE 3000

CMD ["bin/log_ingestor_backend"]
