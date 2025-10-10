# Essential Maven Goals:

```bash
# Analyze dependencies
./mvnw dependency:tree
./mvnw dependency:analyze
./mvnw dependency:resolve

./mvnw clean validate -U
./mvnw buildplan:list-plugin
./mvnw buildplan:list-phase
./mvnw help:all-profiles
./mvnw help:active-profiles
./mvnw license:third-party-report

# Clean the project
./mvnw clean

# Clean and package in one command
./mvnw clean package

# Run Unit test
MAVEN_OPTS="--enable-preview" ./mvnw clean test

# Run integration tests
MAVEN_OPTS="--enable-preview" ./mvnw clean verify

MAVEN_OPTS="--enable-preview" ./mvnw clean verify -Pe2e

# Check for dependency updates
./mvnw versions:display-property-updates
./mvnw versions:display-dependency-updates
./mvnw versions:display-plugin-updates

# Generate project reports
./mvnw site
jwebserver -p 8005 -d "$(pwd)/target/site/"

# JMH Benchmarks
# Build JMH benchmarks
MAVEN_OPTS="--enable-preview" ./mvnw clean package -Pjmh -DskipTests

# List all available JMH benchmarks
java -jar target/jmh-benchmarks.jar -l

# Run benchmarks using Maven exec plugin (recommended)
# Mock website testing (default - WireMock with controlled latency)
MAVEN_OPTS="--enable-preview" ./mvnw exec:java -Pjmh -Dexec.mainClass="info.jab.crawler.benchmarks.CrawlerBenchmark"

# Real website testing (live https://jabrena.github.io/cursor-rules-java/)
MAVEN_OPTS="--enable-preview" ./mvnw exec:java -Pjmh -Dexec.mainClass="info.jab.crawler.benchmarks.CrawlerBenchmark" -Dbenchmark.mode=real

# Explicit mock website testing
MAVEN_OPTS="--enable-preview" ./mvnw exec:java -Pjmh -Dexec.mainClass="info.jab.crawler.benchmarks.CrawlerBenchmark" -Dbenchmark.mode=mock

# Run benchmarks using JAR directly (alternative method)
# Mock website testing
java --enable-preview -jar target/jmh-benchmarks.jar info.jab.crawler.benchmarks.CrawlerBenchmark -rf json -rff src/jmh/test/resources/$(date +"%Y%m%d-%H%M")-mock-jmh-results.json

# Real website testing
java --enable-preview -jar target/jmh-benchmarks.jar info.jab.crawler.benchmarks.CrawlerBenchmark -rf json -rff src/jmh/test/resources/$(date +"%Y%m%d-%H%M")-real-jmh-results.json -Dbenchmark.mode=real

# Run all crawler benchmarks with JFR profiling enabled (mock mode)
java --enable-preview -jar target/jmh-benchmarks.jar info.jab.crawler.benchmarks.CrawlerBenchmark -rf json -rff src/jmh/test/resources/$(date +"%Y%m%d-%H%M")-mock-jmh-results.json -prof jfr

# Run all crawler benchmarks with JFR profiling enabled (real mode)
java --enable-preview -jar target/jmh-benchmarks.jar info.jab.crawler.benchmarks.CrawlerBenchmark -rf json -rff src/jmh/test/resources/$(date +"%Y%m%d-%H%M")-real-jmh-results.json -prof jfr -Dbenchmark.mode=real

# Run all crawler benchmarks with JFR profiling and custom settings
java --enable-preview -jar target/jmh-benchmarks.jar info.jab.crawler.benchmarks.CrawlerBenchmark -rf json -rff src/jmh/test/resources/$(date +"%Y%m%d-%H%M")-jmh-results.json -prof jfr:dir=src/jmh/test/resources

# Note: JMH 1.37 has partial compatibility issues with Java 25 preview features
# Some benchmarks may fail with "ForkedMain" errors when using forked execution
# However, the benchmark mode selection (mock/real) works correctly
# The benchmark shows "Benchmark Mode: REAL" and targets the correct website
# Results are saved in: src/jmh/test/resources/jmh-crawler-benchmark-results.json
#
# WORKING SOLUTION: Use single-threaded execution to avoid forked execution issues
# Add -f 1 to run benchmarks in single-threaded mode:
# java --enable-preview -jar target/jmh-benchmarks.jar info.jab.crawler.benchmarks.CrawlerBenchmark -f 1 -rf json -rff results.json

# Docker JMH Benchmarks
# Build Docker image with GraalVM Java 25
docker build -t java-crawler-jmh .
docker build --no-cache -t java-crawler-jmh .

# Run JMH benchmarks in Docker container with volume mount for results
# Real website testing (default - runs against https://jabrena.github.io/cursor-rules-java/)
# This command will generate both JMH JSON results and JFR profiling files
docker run --rm -v $(pwd)/src/jmh/test/resources:/app/results java-crawler-jmh

# Alternative: Run with explicit command override (single-threaded to avoid forked execution issues)
# Real website testing with JMH reports and JFR profiling
docker run --rm -v $(pwd)/src/jmh/test/resources:/app/results java-crawler-jmh java --enable-preview -Dbenchmark.mode=real -jar jmh-benchmarks.jar info.jab.crawler.benchmarks.CrawlerBenchmark -rf json -rff /app/results/$(date +%Y%m%d-%H%M)-real-jmh-results.json -prof jfr:dir=/app/results

# The JSON results and JFR files will be saved to: src/jmh/test/resources/
# - JMH JSON results: src/jmh/test/resources/YYYYMMDD-HHMM-real-jmh-results.json
# - JFR profiling files: src/jmh/test/resources/info.jab.crawler.benchmarks.CrawlerBenchmark.benchmark*Crawler-AverageTime/profile.jfr
```
