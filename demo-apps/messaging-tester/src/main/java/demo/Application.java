package demo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.Environment;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Hashtable;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.camel.component.hystrix.metrics.servlet.HystrixEventStreamServlet;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsConfiguration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@SpringBootApplication
@EnableWebSocket
// @ImportResource({"classpath:spring/camel-context.xml"})
public class Application implements WebSocketConfigurer{

    // must have a main method spring-boot can run
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    ServletRegistrationBean hystrixServletRegistrationBean() {
        ServletRegistrationBean mapping = new ServletRegistrationBean();
        mapping.setServlet(new HystrixEventStreamServlet());
        mapping.addUrlMappings("/hystrix.stream");
        mapping.setName("HystrixEventStreamServlet");

        return mapping;
    }

    @Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		
		// Expose endpoint and add Handler. Wildcard allowed origins to support COORS
		// registry.addHandler(new WebsocketTextHandler(), "/websocket").setAllowedOrigins("*");
		registry.addHandler(new WebSocketTextHandler(), "/websocket");
    }
    

      
    @Autowired
    private Environment env;
    private final static String CNX_FACTORY_NAME = "default";

    @Bean
    CachingConnectionFactory cachingConnectionFactory() throws JMSException, NamingException {
        Hashtable<String,String>  jndiConfig = new Hashtable<String,String>();
        
        jndiConfig.put(InitialContext.INITIAL_CONTEXT_FACTORY, env.getProperty("jms."+InitialContext.INITIAL_CONTEXT_FACTORY));
        jndiConfig.put("connectionFactory."+CNX_FACTORY_NAME, env.getProperty("jms.connectionFactory."+CNX_FACTORY_NAME));
        jndiConfig.put(Context.PROVIDER_URL, env.getProperty("jms."+InitialContext.PROVIDER_URL));
        jndiConfig.put(Context.SECURITY_PRINCIPAL, env.getProperty("jms."+InitialContext.SECURITY_PRINCIPAL));
        jndiConfig.put(Context.SECURITY_CREDENTIALS, env.getProperty("jms."+InitialContext.SECURITY_CREDENTIALS));


        Context context = new InitialContext(jndiConfig);
        ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup(env.getProperty("jms.default.cnx.factory"));

        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(connectionFactory);
        return cachingConnectionFactory;
    }

    @Bean
    public JmsComponent jms(CachingConnectionFactory cachingConnectionFactory) {

        JmsConfiguration jmsConfiguration =new JmsConfiguration(cachingConnectionFactory);
        jmsConfiguration.setCacheLevelName(env.getProperty("jms.cache.level"));

        JmsComponent jmsComponent = new JmsComponent(jmsConfiguration);

        return jmsComponent;
    }

}