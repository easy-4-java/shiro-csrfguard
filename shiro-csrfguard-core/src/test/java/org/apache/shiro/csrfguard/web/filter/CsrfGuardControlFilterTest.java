package org.apache.shiro.csrfguard.web.filter;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.csrfguard.CsrfGuard;
import org.owasp.csrfguard.CsrfGuardHttpSessionListener;
import org.owasp.csrfguard.config.ConfigurationProvider;
import org.owasp.csrfguard.config.ConfigurationProviderFactory;
import org.owasp.csrfguard.config.PropertiesConfigurationProvider;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpSessionEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证 Jakarta 迁移后实际 CSRF 过滤器仍执行拒绝与放行规则。 */
class CsrfGuardControlFilterTest {

    private Properties properties;
    private CsrfGuardControlFilter filter;

    @BeforeEach
    void configureGuard() {
        properties = new Properties();
        properties.setProperty("org.owasp.csrfguard.configuration.provider.factory", FreshConfigurationFactory.class.getName());
        properties.setProperty("org.owasp.csrfguard.Enabled", "true");
        properties.setProperty("org.owasp.csrfguard.TokenName", "TEST_CSRF_TOKEN");
        properties.setProperty("org.owasp.csrfguard.TokenPerPage", "false");
        properties.setProperty("org.owasp.csrfguard.ProtectedMethods", "POST");
        properties.setProperty("org.owasp.csrfguard.ValidateWhenNoSessionExists", "true");
        properties.setProperty("org.owasp.csrfguard.LogicalSessionExtractor",
                "org.owasp.csrfguard.session.SessionTokenKeyExtractor");
        properties.setProperty("org.owasp.csrfguard.action.Error", "org.owasp.csrfguard.action.Error");
        properties.setProperty("org.owasp.csrfguard.action.Error.Code", "403");
        properties.setProperty("org.owasp.csrfguard.action.Error.Message", "Invalid CSRF token");
        CsrfGuard.load(properties);
        filter = new CsrfGuardControlFilter();
        filter.setFilterConfig(new MockFilterConfig());
    }

    @AfterEach
    void clearGuard() {
        Properties disabled = new Properties();
        disabled.setProperty("org.owasp.csrfguard.Enabled", "false");
        disabled.setProperty("org.owasp.csrfguard.configuration.provider.factory", FreshConfigurationFactory.class.getName());
        CsrfGuard.load(disabled);
        filter.destroy();
    }

    @Test
    void rejectsMissingTokenBeforeApplicationChain() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();
        filter.executeChain(new MockHttpServletRequest("POST", "/protected"), response,
                (request, result) -> invoked.set(true));

        assertFalse(invoked.get());
        assertEquals(403, response.getStatus());
    }

    @Test
    void rejectsIncorrectSessionToken() throws Exception {
        MockHttpServletRequest request = requestWithSession();
        request.setParameter("TEST_CSRF_TOKEN", "incorrect-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();
        filter.executeChain(request, response, (input, output) -> invoked.set(true));

        assertFalse(invoked.get());
        assertEquals(403, response.getStatus());
    }

    @Test
    void allowsRequestWithValidSessionToken() throws Exception {
        MockHttpServletRequest request = requestWithSession();
        String token = CsrfGuard.getInstance().getTokenService()
                .generateTokensIfAbsent(request.getSession().getId(), "POST", "/protected");
        request.setParameter("TEST_CSRF_TOKEN", token);
        AtomicBoolean invoked = new AtomicBoolean();
        filter.executeChain(request, new MockHttpServletResponse(), (input, output) -> invoked.set(true));

        assertTrue(invoked.get());
    }

    @Test
    void allowsRequestWhenProtectionIsExplicitlyDisabled() throws Exception {
        properties.setProperty("org.owasp.csrfguard.Enabled", "false");
        CsrfGuard.load(properties);
        AtomicBoolean invoked = new AtomicBoolean();
        filter.executeChain(new MockHttpServletRequest("POST", "/protected"), new MockHttpServletResponse(),
                (request, response) -> invoked.set(true));

        assertTrue(invoked.get());
    }

    private MockHttpServletRequest requestWithSession() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/protected");
        new CsrfGuardHttpSessionListener().sessionCreated(new HttpSessionEvent(request.getSession()));
        return request;
    }
    /** 每个测试使用独立配置，隔离上游默认工厂的静态缓存。 */
    public static final class FreshConfigurationFactory implements ConfigurationProviderFactory {
        @Override
        public ConfigurationProvider retrieveConfiguration(Properties properties) {
            return new PropertiesConfigurationProvider(properties);
        }
    }
}

