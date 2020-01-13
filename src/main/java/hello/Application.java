package hello;


import datadog.opentracing.DDTracer;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;


@SpringBootApplication
public class Application {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

    public static void main(String[] args) {


        // Initialize the tracer from environment variables or system properties
        Tracer tracer = new DDTracer();
        GlobalTracer.register(tracer);
//        // register the same tracer with the Datadog API
        datadog.trace.api.GlobalTracer.registerIfAbsent((datadog.trace.api.Tracer) tracer);

        SpringApplication.run(Application.class, args);
    }
}
