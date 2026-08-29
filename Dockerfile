FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# As dependencias mudam bem menos que o codigo: resolve-las numa camada propria faz o
# build seguinte reaproveitar o cache em vez de baixar tudo de novo a cada commit.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
# A suite roda no CI, com relatorio. Repeti-la aqui so alongaria o build da imagem.
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Usuario sem privilegios: um processo que so precisa ler o proprio jar nao tem por que
# ser root dentro do container.
RUN useradd --system --uid 10001 --create-home appuser
USER appuser

COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
