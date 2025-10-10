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

# Create results directory for both JSON and JFR files
RUN mkdir -p /app/results

# Usage Examples:
# Real website testing: docker run -v $(pwd)/src/jmh/test/resources:/app/results java-crawler-jmh
# This will run benchmarks against https://jabrena.github.io/cursor-rules-java/

# Set the default command to run JMH benchmarks with JSON output and JFR profiling
# The results will be written to /app/results with date-based naming
# JFR files will be generated in /app/results/ subdirectories
# Default mode is mock (WireMock with controlled latency)
# Use -e BENCHMARK_MODE=real to test against live website
# Set the default command to run JMH benchmarks against real website with JSON output and JFR profiling
# The results will be written to /app/results with date-based naming
# JFR files will be generated in /app/results/ subdirectories
# This runs against the real website: https://jabrena.github.io/cursor-rules-java/
# Set environment variable for real website testing
ENV BENCHMARK_MODE=real

# Create a simple script to handle the benchmark execution
RUN printf '#!/bin/sh\n' > /app/run-benchmark.sh && \
    printf 'java --enable-preview -Dbenchmark.mode=real -jar jmh-benchmarks.jar info.jab.crawler.benchmarks.CrawlerBenchmark -rf json -rff /app/results/$(date +%%Y%%m%%d-%%H%%M)-real-jmh-results.json -prof jfr:dir=/app/results\n' >> /app/run-benchmark.sh && \
    chmod +x /app/run-benchmark.sh

CMD ["/app/run-benchmark.sh"]
