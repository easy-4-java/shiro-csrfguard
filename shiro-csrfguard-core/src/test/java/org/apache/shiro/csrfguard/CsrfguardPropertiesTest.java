package org.apache.shiro.csrfguard;

import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.owasp.csrfguard.config.PropertiesConfigurationProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证对外导出的 Properties 能被 CSRFGuard 4 的配置解析器直接读取。 */
class CsrfguardPropertiesTest {

    @Test
    void defaultConfigurationOmitsUnsetOptionalValues() {
        Properties exported = new CsrfguardProperties().toProperties();

        assertFalse(exported.containsKey("org.owasp.csrfguard.NewTokenLandingPage"));
        assertFalse(new PropertiesConfigurationProvider(exported).isEnabled());
    }

    @Test
    void enabledConfigurationLoadsTokenAndSessionSettings() {
        CsrfguardProperties properties = new CsrfguardProperties();
        properties.setEnabled(true);
        properties.setProtectedMethods(Set.of("POST"));
        PropertiesConfigurationProvider provider = new PropertiesConfigurationProvider(properties.toProperties());

        assertTrue(provider.isEnabled());
        assertEquals("OWASP_CSRFGUARD", provider.getTokenName());
        assertEquals(32, provider.getTokenLength());
        assertEquals("org.owasp.csrfguard.session.SessionTokenKeyExtractor",
                provider.getLogicalSessionExtractor().getClass().getName());
    }

    @Test
    void exportsConfiguredLandingPageAsText() {
        CsrfguardProperties properties = new CsrfguardProperties();
        properties.setNewTokenLandingPage("/new-session");

        assertEquals("/new-session",
                properties.toProperties().getProperty("org.owasp.csrfguard.NewTokenLandingPage"));
        assertEquals("false", properties.toProperties().getProperty("org.owasp.csrfguard.Enabled"));
    }
}

