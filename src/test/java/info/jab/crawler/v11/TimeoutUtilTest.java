package info.jab.crawler.v11;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for TimeoutUtil implementing timeout-as-method pattern.
 *
 * These tests verify the timeout utility that addresses the SoftwareMill critique
 * by implementing timeout as a method rather than a configuration parameter.
 */
class TimeoutUtilTest {

    @Test
    @DisplayName("TimeoutUtil should complete successfully when task finishes before timeout")
    void should_completeSuccessfully_when_taskFinishesBeforeTimeout() throws Exception {
        // Given
        Duration timeout = Duration.ofMillis(1000);
        String expectedResult = "success";

        // When
        String result = TimeoutUtil.timeout(timeout, () -> {
            Thread.sleep(100); // Simulate work that completes quickly
            return expectedResult;
        });

        // Then
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    @DisplayName("TimeoutUtil should throw TimeoutException when task exceeds timeout")
    void should_throwTimeoutException_when_taskExceedsTimeout() {
        // Given
        Duration timeout = Duration.ofMillis(100);
        String expectedResult = "success";

        // When & Then
        assertThatThrownBy(() -> TimeoutUtil.timeout(timeout, () -> {
            Thread.sleep(500); // Simulate work that takes longer than timeout
            return expectedResult;
        }))
        .isInstanceOf(TimeoutException.class)
        .hasMessageContaining("Task timed out after " + timeout);
    }

    @Test
    @DisplayName("TimeoutUtil should propagate task exceptions")
    void should_propagateTaskExceptions() {
        // Given
        Duration timeout = Duration.ofMillis(1000);
        RuntimeException expectedException = new RuntimeException("Task failed");

        // When & Then
        assertThatThrownBy(() -> TimeoutUtil.timeout(timeout, () -> {
            throw expectedException;
        }))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Task failed");
    }

    @Test
    @DisplayName("TimeoutUtil should handle InterruptedException")
    void should_handleInterruptedException() {
        // Given
        Duration timeout = Duration.ofMillis(1000);

        // When & Then
        assertThatThrownBy(() -> TimeoutUtil.timeout(timeout, () -> {
            Thread.currentThread().interrupt();
            Thread.sleep(100);
            return "result";
        }))
        .isInstanceOf(InterruptedException.class);
    }

    @Test
    @DisplayName("TimeoutUtil should work with very short timeouts")
    void should_workWithVeryShortTimeouts() throws Exception {
        // Given
        Duration timeout = Duration.ofMillis(1);
        String expectedResult = "quick";

        // When
        String result = TimeoutUtil.timeout(timeout, () -> expectedResult);

        // Then
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    @DisplayName("TimeoutUtil should work with longer timeouts")
    @Timeout(3) // Ensure test doesn't hang
    void should_workWithLongerTimeouts() throws Exception {
        // Given
        Duration timeout = Duration.ofMillis(2000);
        String expectedResult = "delayed";

        // When
        String result = TimeoutUtil.timeout(timeout, () -> {
            Thread.sleep(100); // Complete well before timeout
            return expectedResult;
        });

        // Then
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    @DisplayName("TimeoutUtil should handle null return values")
    void should_handleNullReturnValues() throws Exception {
        // Given
        Duration timeout = Duration.ofMillis(1000);

        // When
        String result = TimeoutUtil.timeout(timeout, () -> null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("TimeoutUtil should handle complex return types")
    void should_handleComplexReturnTypes() throws Exception {
        // Given
        Duration timeout = Duration.ofMillis(1000);
        TestData expectedData = new TestData("test", 42);

        // When
        TestData result = TimeoutUtil.timeout(timeout, () -> expectedData);

        // Then
        assertThat(result).isEqualTo(expectedData);
        assertThat(result.name()).isEqualTo("test");
        assertThat(result.value()).isEqualTo(42);
    }

    /**
     * Test data class for complex return type testing.
     */
    private record TestData(String name, int value) {}
}
