package org.apache.shiro.csrfguard.spring;

import java.util.Properties;
import java.util.Map;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.ServletContextEvent;
import org.junit.jupiter.api.Test;
import org.owasp.csrfguard.CsrfGuard;
import org.owasp.csrfguard.config.ConfigurationProvider;
import org.owasp.csrfguard.config.ConfigurationProviderFactory;
import org.owasp.csrfguard.config.PropertiesConfigurationProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 验证启动配置校验及原生 Jakarta 会话监听器的实际行为。 */
class ShiroCsrfguardAutoConfigurationTest {

    @Test
    void rejectsInvalidConfigurationDuringBeanCreation() {
        ShiroCsrfguardProperties properties = properties();
        properties.setTokenLength(2);

        assertThrows(RuntimeException.class,
                () -> new ShiroCsrfguardAutoConfiguration().csrfGuard(properties));
    }

    @Test
    void registeredListenerCreatesSessionToken() throws Exception {
        ShiroCsrfguardAutoConfiguration configuration = new ShiroCsrfguardAutoConfiguration();
        CsrfGuard guard = configuration.csrfGuard(properties());
        MockHttpSession session = new MockHttpSession();

        configuration.csrfGuardHttpSessionListener().getListener().sessionCreated(new HttpSessionEvent(session));

        assertNotNull(guard.getTokenService().getTokenValue(session.getId(), "/protected"));
    }

    @Test
    void servletContextInitializationRetainsSpringConfigurationWithoutExternalFile() {
        ShiroCsrfguardProperties properties = properties();
        properties.setTokenName("SPRING_TOKEN");
        properties.getProtectedPages().put("save", "%servletContext%/save");
        MockServletContext servletContext = new MockServletContext();
        servletContext.setContextPath("/console");
        StaticWebApplicationContext applicationContext = new StaticWebApplicationContext();
        applicationContext.setServletContext(servletContext);
        ShiroCsrfguardAutoConfiguration configuration = new ShiroCsrfguardAutoConfiguration();
        configuration.setApplicationContext(applicationContext);
        configuration.csrfGuard(properties);

        configuration.csrfGuardServletContextListener().contextInitialized(new ServletContextEvent(servletContext));

        assertEquals("SPRING_TOKEN", CsrfGuard.getInstance().getTokenName());
        org.junit.jupiter.api.Assertions.assertTrue(CsrfGuard.getInstance().getProtectedPages().contains("/console/save"));
    }

    @Test
    void requestingServletListenerInitializesGuardFirst() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
                    Map.of("shiro.csrfguard.enabled", "true")));
            context.register(ShiroCsrfguardAutoConfiguration.class);
            context.addBeanFactoryPostProcessor(factory -> {
                for (String name : factory.getBeanDefinitionNames()) {
                    factory.getBeanDefinition(name).setLazyInit(true);
                }
            });
            context.refresh();
            assertFalse(context.getBeanFactory().containsSingleton("csrfGuard"));

            context.getBean("csrfGuardServletContextListener");

            assertTrue(context.getBeanFactory().containsSingleton("csrfGuard"));
        }
    }

    private ShiroCsrfguardProperties properties() {
        ShiroCsrfguardProperties properties = new ShiroCsrfguardProperties() {
            @Override
            public Properties toProperties() {
                Properties properties = super.toProperties();
                properties.setProperty("org.owasp.csrfguard.configuration.provider.factory",
                        FreshConfigurationFactory.class.getName());
                return properties;
            }
        };
        properties.setEnabled(true);
        return properties;
    }

    /** 隔离上游配置工厂的全局静态缓存。 */
    public static final class FreshConfigurationFactory implements ConfigurationProviderFactory {
        @Override
        public ConfigurationProvider retrieveConfiguration(Properties properties) {
            return new PropertiesConfigurationProvider(properties);
        }
    }
}

