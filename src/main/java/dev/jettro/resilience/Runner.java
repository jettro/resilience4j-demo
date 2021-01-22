package dev.jettro.resilience;

import java.time.LocalDateTime;

public class Runner {
    public static void main(String[] args) {
        EndpointService service = new EndpointService();

        for (int i=0; i < 50; i++) {
            service.call("jettro_" + i, LocalDateTime.now());
            service.call("error", LocalDateTime.now());
            service.call("slow", LocalDateTime.now());
        }
    }
}
