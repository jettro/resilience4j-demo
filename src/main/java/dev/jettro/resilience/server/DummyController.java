package dev.jettro.resilience.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/dummy")
public class DummyController {
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyController.class);

    @GetMapping("/error")
    public String error() {
        LOGGER.info("Receive request for '/error'");
        throw new IllegalArgumentException();
    }

    @GetMapping("/slow")
    public String slow() {
        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        return "SLOW";
    }

    @GetMapping("/veryslow")
    public String verySlow() {
        try {
            TimeUnit.MINUTES.sleep(1);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        return "SLOW";
    }

    @GetMapping("/")
    public String get() {
        return "OK";
    }
}
