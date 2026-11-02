# API de Dulces Aromas (RNF21). Se construye desde backend/:
#   docker build -t dulces-aromas-api .
#   docker run -p 8080:8080 --env-file .env dulces-aromas-api
# La configuración entra solo por variables de entorno (DB_URL, DB_USER, DB_PASSWORD, BREVO_CLAVE…); la imagen no
# lleva credenciales. Las pruebas no corren aquí: corren antes, con ./mvnw test.

# --- Construcción -------------------------------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS construccion
WORKDIR /fuente

# Primero las dependencias, para que Docker las reutilice mientras no cambie pom.xml.
COPY mvnw pom.xml ./
COPY .mvn .mvn
# En Windows, Git puede dejar el wrapper con fin de línea CRLF, que sh no entiende.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src src
RUN ./mvnw -q -B -DskipTests package && cp target/*.jar /fuente/api.jar

# --- Ejecución ----------------------------------------------------------------------------------------------------
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --no-create-home api
COPY --from=construccion /fuente/api.jar api.jar
USER api

# Los planes gratuitos dan unos 512 MB: la JVM usa como mucho el 75 % y un recolector de memoria pequeño.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "api.jar"]
