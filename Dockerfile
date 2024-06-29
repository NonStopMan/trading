# Use the official Gradle image to build the application
FROM gradle:jdk22 AS build

# Set the working directory
WORKDIR /home/gradle/project

# Copy the entire project to the working directory
COPY . .

# Build the project
RUN gradle installDist --no-daemon

# Use the OpenJDK image to run the application
FROM openjdk:22-jdk-slim

# Metadata as labels
LABEL maintainer="mohammed.thabet@outlook.com"
LABEL version="1.1.4"
LABEL description="Candlestick backend service"

# Create a non-root user
RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup

# Copy the built fat JAR file from the build stage
COPY --from=build /home/gradle/project/build/install/candlesticks /app/candlesticks


# Set permissions and switch to non-root user
RUN chown -R appuser:appgroup /app
USER appuser

# Set the entry point
ENTRYPOINT ["/app/candlesticks/bin/candlesticks"]
