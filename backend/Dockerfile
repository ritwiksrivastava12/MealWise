FROM eclipse-temurin:21-jre AS run
WORKDIR /app
COPY build/libs/mealwise-api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
