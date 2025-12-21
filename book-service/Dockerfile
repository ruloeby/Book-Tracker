FROM maven:3.9.4-eclipse-temurin-17 AS build

WORKDIR /workspace

#copy pom first to cache dependency download
COPY pom.xml .

# if you have .mvn and mvnw in repo, copy them too:
COPY .mvn .mvn
# download dependencies (speed up rebuilds)
RUN mvn -B dependency:go-offline

#copy source and package
COPY src ./src
RUN mvn -B -DskipTests package

# ---- runtime stage ----
FROM eclipse-temurin:17-jre-jammy
ARG JAR_FILE=target/*.jar

# copy jar from build stage
COPY --from=build /workspace/${JAR_FILE} /app/app.jar

# create non-root user (optional but recommended)
RUN addgroup --system app && adduser --system --ingroup app app && chown app:app /app
USER app

EXPOSE 8081

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]