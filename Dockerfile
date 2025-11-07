# --- Build ----
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

# copy source and build (skip tests only in the image build)
COPY src/ src/
RUN ./gradlew --no-daemon clean bootJar -x test

# ---- Runtime ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]