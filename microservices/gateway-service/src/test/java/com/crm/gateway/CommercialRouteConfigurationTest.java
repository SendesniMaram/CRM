package com.crm.gateway;
import org.junit.jupiter.api.Test;import org.springframework.boot.env.YamlPropertySourceLoader;import org.springframework.core.io.ClassPathResource;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
/** Static resolved route values only; no Gateway server or downstream network. */
class CommercialRouteConfigurationTest {
 @Test void commercialNamespaceHasOneExplicitRouteAndOnlyStripsTheGatewayPrefix()throws Exception{var sources=new YamlPropertySourceLoader().load("candidate",new ClassPathResource("application.yml"));int matches=0;for(var source:sources)for(int i=0;i<100;i++){String prefix="spring.cloud.gateway.routes["+i+"]";Object id=source.getProperty(prefix+".id");if("customer-contact-work".equals(id)){matches++;assertEquals("lb://CUSTOMER-SERVICE",source.getProperty(prefix+".uri"));assertEquals("Path=/customer/api/contact-work/**",source.getProperty(prefix+".predicates[0]"));assertEquals("StripPrefix=1",source.getProperty(prefix+".filters[0]"));assertNull(source.getProperty(prefix+".filters[1]"));}}assertEquals(1,matches);}
}
