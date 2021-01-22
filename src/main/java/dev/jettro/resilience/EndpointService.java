package dev.jettro.resilience;

import com.sun.jersey.api.client.UniformInterfaceException;
import dev.jettro.resilience.dummy.DummyEndpoint;
import dev.jettro.resilience.dummy.DummyException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Supplier;

public class EndpointService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointService.class);
    private final DummyEndpoint dummyEndpoint;
    private final CircuitBreaker circuitBreaker;

    public EndpointService() {
        this.dummyEndpoint = new DummyEndpoint();
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(10) // 10% of requests result in an error
                .slowCallRateThreshold(50) // 50% of calls are to slow
                .waitDurationInOpenState(Duration.ofMillis(10)) // Wait 10 milliseconds to go into half open state
                .slowCallDurationThreshold(Duration.ofMillis(50)) // After 50 milliseconds of response time a call is slow
                .permittedNumberOfCallsInHalfOpenState(5) // Do a maximum of 5 calls in half open state
                .minimumNumberOfCalls(10) // Have at least 10 calls in the window to calculate rates
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED) // Use time based, not number based
                .slidingWindowSize(5) // Record 5 seconds of requests for the window
                .recordExceptions(UniformInterfaceException.class) // Exception thrown by REST client, used as failure
                .ignoreExceptions(DummyException.class) // Business exception that is not a failure for circuit breaker
                .build();
        this.circuitBreaker = CircuitBreaker.of("dummyBreaker", circuitBreakerConfig);
        addLogging(this.circuitBreaker);
    }

    /**
     * Create a call to the endpoint that usually wraps the remote service.
     *
     * @param message   String containing the message to pass
     * @param timestamp LocalDateTime to
     */
    public void call(String message, LocalDateTime timestamp) {
        Supplier<String> stringSupplier = circuitBreaker.decorateSupplier(
                () -> dummyEndpoint.executeCall(message, LocalDateTime.now())
        );

        try {
            String test = stringSupplier.get();
            LOGGER.info("{}: {}", timestamp.toString(), test);
        } catch (UniformInterfaceException e) {
            LOGGER.info("We have found an exception with message: {}", message);
        } catch (CallNotPermittedException e) {
            LOGGER.info("The circuitbreaker is now Open, so calls are not permitted");
        }
        printMetrics();
    }

    private void printMetrics() {
        CircuitBreaker.Metrics metrics = this.circuitBreaker.getMetrics();

        String rates = String.format("Rate of failures: %.2f, slow calls: %.2f",
                metrics.getFailureRate(),
                metrics.getSlowCallRate());

        String calls = String.format("Calls: success %d, failed %d, not permitted %d, buffered %d",
                metrics.getNumberOfSuccessfulCalls(),
                metrics.getNumberOfFailedCalls(),
                metrics.getNumberOfNotPermittedCalls(),
                metrics.getNumberOfBufferedCalls()
        );

        String slow = String.format("Slow: total %d, success %d, failed %d",
                metrics.getNumberOfSlowCalls(),
                metrics.getNumberOfSlowFailedCalls(),
                metrics.getNumberOfSlowSuccessfulCalls()
        );

        LOGGER.info(rates);
        LOGGER.info(calls);
        LOGGER.info(slow);
    }

    private void addLogging(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher()
                .onSuccess(event -> LOGGER.info("SUCCESS"))
                .onError(event -> LOGGER.info("ERROR - {}", event.getThrowable().getMessage()))
                .onIgnoredError(event -> LOGGER.info("IGNORED_ERROR - {}", event.getThrowable().getMessage()))
                .onReset(event -> LOGGER.info("RESET"))
                .onStateTransition(event -> LOGGER.info("STATE_TRANSITION - {} > {}",
                        event.getStateTransition().getFromState(), event.getStateTransition().getToState()));
    }
}
