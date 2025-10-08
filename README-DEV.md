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
./mvnw clean test

# Run integration tests
./mvnw clean verify

./mvnw clean verify -Pe2e

# Check for dependency updates
./mvnw versions:display-property-updates
./mvnw versions:display-dependency-updates
./mvnw versions:display-plugin-updates

# Generate project reports
./mvnw site
jwebserver -p 8005 -d "$(pwd)/target/site/"

# JMH Benchmarks
# Build JMH benchmarks
./mvnw clean package -Pjmh

# List all available JMH benchmarks
java -jar target/jmh-benchmarks.jar -l

# Run all crawler benchmarks and save results to JSON
java --enable-preview -jar target/jmh-benchmarks.jar info.jab.crawler.benchmarks.CrawlerBenchmark -rf json -rff src/jmh/test/resources/jmh-crawler-benchmark-results.json

# Note: JMH 1.37 has partial compatibility issues with Java 25 preview features
# Some benchmarks may fail with "InfraControl" errors when using forked execution
# However, several benchmarks (ActorCrawler, MultiThreadedRecursiveCrawler, etc.) run successfully
# Results are saved in: src/jmh/test/resources/jmh-crawler-benchmark-results.json
```

