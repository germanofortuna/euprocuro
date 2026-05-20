FROM maven:3.9.9-eclipse-temurin-11 AS build
WORKDIR /app

# Copia só o pom.xml primeiro para cachear o download das dependencias
# Essa camada só e refeita quando o pom.xml muda
COPY pom.xml .
RUN mvn -B dependency:go-offline -q

COPY src ./src
RUN mvn -B clean package -DskipTests -o

FROM eclipse-temurin:11-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
