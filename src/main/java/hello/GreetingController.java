package hello;




import datadog.opentracing.DDTracer;
import datadog.trace.api.CorrelationIdentifier;
import datadog.trace.api.DDTags;
import io.opentracing.util.GlobalTracer;
import io.opentracing.Tracer;
import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.propagation.TextMapInjectAdapter;
import io.opentracing.tag.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


@RestController
public class GreetingController {


    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    HttpServletRequest request;

    private static final Logger logger = LoggerFactory.getLogger(GreetingController.class);

    @Value("#{environment['sleeptime'] ?: '1000'}")
    private long sleepTime;

    private Tracer tracer;


    @RequestMapping("/ServiceC")
    public String serviceC() throws InterruptedException {

        //tracer = (DDTracer)GlobalTracer.get();
        tracer = GlobalTracer.get();

        String rs;

        // Hashmap containing Header key/val
        Map<String, String> map = new HashMap<>();
        //build HttpHeader
        HttpHeaders header = new HttpHeaders();


        try (Scope scope = tracer.buildSpan("ServiceC").startActive(true)) {
            scope.span().setTag(DDTags.SERVICE_NAME, "springtest0");

            tracer.inject(scope.span().context(), Format.Builtin.HTTP_HEADERS, new TextMapInjectAdapter(map));
            header.setAll(map);

            try {

                // Manually injecting B3 Headers format into logs
                MDC.put("dd.trace_id", String.valueOf((new BigInteger(CorrelationIdentifier.getTraceId())).toString(16).toLowerCase()));
                MDC.put("dd.span_id", String.valueOf((new BigInteger(CorrelationIdentifier.getSpanId())).toString(16).toLowerCase()));

                // Manually inject Datadog Header format into logs
                //MDC.put("dd.trace_id", String.valueOf(CorrelationIdentifier.getTraceId()));
                //MDC.put("dd.span_id", String.valueOf(CorrelationIdentifier.getSpanId()));

                doSomeStuff(scope, "Hello");
                //doSomeStuff("Hello");
                Thread.sleep(sleepTime);
                //doSomeOtherStuff("World!");
                doSomeOtherStuff(scope, "World!");
                logger.info("In Service C ***************");

            } finally {
                MDC.remove("dd.trace_id");
                MDC.remove("dd.span_id");
            }


            //Post to downstream service
            rs = restTemplate.postForEntity("http://localhost:9393/ServiceD", new HttpEntity(header), String.class).getBody();
        }


        return rs;
    }


    @RequestMapping("/ServiceD")
    public String serviceD() throws InterruptedException {

        tracer = GlobalTracer.get();

        Enumeration<String> e = request.getHeaderNames();
        Map<String, String> spanMap = new HashMap<>();


        while (e.hasMoreElements()) {
            // add the names of the request headers into the spanMap
            String key = e.nextElement();
            String value = request.getHeader(key);
            spanMap.put(key, value);
        }


        Tracer.SpanBuilder spanBuilder;
        String operationName = "ServiceD";
        try {
            SpanContext parentSpan = tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(spanMap));
            if (parentSpan == null) {
                spanBuilder = tracer.buildSpan(operationName);
            } else {
                spanBuilder = tracer.buildSpan(operationName).asChildOf(parentSpan);
            }
        } catch (IllegalArgumentException ex) {
            spanBuilder = tracer.buildSpan(operationName);
        }


        try (Scope scope = spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER).startActive(true)) {

            scope.span().setTag(DDTags.SERVICE_NAME, "springtest0");

            Thread.sleep(230L);
            logger.info("In Service D ***************");

        }


        return "Service D\n";
    }



    private String doSomeStuff(Scope scope, String somestring) throws InterruptedException {
        String astring;
        try (Scope scope1 = tracer.buildSpan("doSomeStuff").asChildOf(scope.span()).startActive(true)) {
            scope1.span().setTag(DDTags.SERVICE_NAME, "springtest0");
            astring = String.format("Hello, %s!", somestring);
            Thread.sleep(sleepTime);
            logger.info("In doSomeStuff()");
        }
        return astring;

    }

    private void doSomeOtherStuff(Scope scope, String somestring) throws InterruptedException {
        try (Scope scope1 = tracer.buildSpan("doSomeOtherStuff").asChildOf(scope.span()).startActive(true)) {
            scope1.span().setTag(DDTags.SERVICE_NAME, "springtest0");
            Thread.sleep(sleepTime);
            logger.info("In doSomeOtherStuff()");
        }
        System.out.println(somestring);
        Thread.sleep(sleepTime);
    }


}
