# Multi-stage Dockerfile for JMH Benchmark Build and Execution
# Uses GraalVM Java 25 for compilation and execution

# Stage 1: Build stage
FROM ghcr.io/graalvm/jdk-community:25 AS builder

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml first for better layer caching
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn/ ./.mvn/

# Make Maven wrapper executable
RUN chmod +x ./mvnw

# Copy source directories
COPY src/ ./src/

# Build the project with JMH profile, skip tests for faster build
RUN ./mvnw clean package -Pjmh -DskipTests

# Stage 2: Runtime stage
FROM ghcr.io/graalvm/jdk-community:25 AS runtime

# Set timezone to Madrid, Spain
ENV TZ=Europe/Madrid
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Set working directory
WORKDIR /app

# Copy the built JMH benchmarks JAR from builder stage
COPY --from=builder /app/target/jmh-benchmarks.jar ./jmh-benchmarks.jar

# Create results directory
RUN mkdir -p /app/results

# Set the default command to run JMH benchmarks with JSON output
# The results will be written to /app/results with date-based naming
CMD ["sh", "-c", "java --enable-preview -jar jmh-benchmarks.jar info.jab.crawler.benchmarks.CrawlerBenchmark -rf json -rff /app/results/$(date +%Y%m%d-%H%M)-jmh-results.json"]
