package dev.jettro.resilience.dummy;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.time.LocalDateTime;

/**
 * The DummyEndpoint is responsible for the connection with the dummy webserver.
 */
public class DummyEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyEndpoint.class);

    public static final String BACKEND_URL = "http://localhost:8080";
    private final WebResource webResource;

    public DummyEndpoint() {
        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);
         this.webResource = client.resource(UriBuilder.fromUri(BACKEND_URL).build());
    }

    /**
     * Calls the backend, uses the content of the message to determine the path to go to.
     *
     * @param message String used to determine the path to connect to.
     * @param timestamp Used as an example to pass metadata
     * @return String giving an indication what happened.
     */
    public String executeCall(String message, LocalDateTime timestamp) {
        if (null == message || message.isBlank()) {
            throw new DummyException("The provided message is empty, we cannot decide where to go");
        }
        LOGGER.info("Call at {}: '{}'", timestamp.toString(), message);

        WebResource webResource = this.webResource.path("/dummy/");
        if (message.equalsIgnoreCase("error")) {
            webResource = webResource.path("/error");
        } else if (message.equalsIgnoreCase("slow")) {
            webResource = webResource.path("/slow");
        } else if (message.equalsIgnoreCase("timeout")) {
            webResource = webResource.path("/veryslow");
        }

        return webResource.accept(MediaType.APPLICATION_JSON).get(String.class);
    }
}
