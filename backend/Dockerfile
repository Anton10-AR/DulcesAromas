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

# Los planes gratuitos dan unos 512 MB y una fracción de procesador. Con el heap al 65 % la API arrancaba al límite
# de la memoria y un despliegue se quedó sin terminar (13/11/2026). Ahora:
# - heap al 55 % y un recolector de memoria pequeño;
# - solo el compilador rápido de la JVM (TieredStopAtLevel=1): arranca antes y usa menos procesador y memoria;
# - topes para el código compilado, las clases cargadas y la pila de cada hilo.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=55 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=40m -XX:MaxMetaspaceSize=150m -Xss512k"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "api.jar"]
