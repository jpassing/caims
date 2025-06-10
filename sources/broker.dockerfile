#
# Build stage.
#
FROM maven:3.9.8-eclipse-temurin-22-alpine AS build
WORKDIR /app

# Copy local code to the container image.
COPY . ./

# Build the JAR
RUN mvn clean package -DskipTests

#
# Package stage.
#
FROM gcr.io/distroless/java17-debian12
WORKDIR /app

COPY --from=build /app/target/confidential-model-serving-1.0.0.jar .

EXPOSE 8080
CMD ["confidential-model-serving-1.0.0.jar"]
