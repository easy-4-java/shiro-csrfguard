package org.apache.shiro.csrfguard.spring;


import org.apache.shiro.csrfguard.CsrfguardConstants;
import org.apache.shiro.csrfguard.CsrfguardJavascriptServletProperties;
import org.apache.shiro.csrfguard.web.filter.CsrfGuardControlFilter;
import org.owasp.csrfguard.CsrfGuard;
import org.owasp.csrfguard.CsrfGuardHttpSessionListener;
import org.owasp.csrfguard.CsrfGuardServletContextListener;
import org.owasp.csrfguard.servlet.JavaScriptServlet;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(org.owasp.csrfguard.CsrfGuard.class)
@ConditionalOnProperty(prefix = ShiroCsrfguardProperties.PREFIX, value = "enabled", havingValue = "true")
@EnableConfigurationProperties(ShiroCsrfguardProperties.class)
/**
 * Auto-configuration for Shiro CSRF Guard integration.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public class ShiroCsrfguardAutoConfiguration implements ApplicationContextAware {

	private ApplicationContext applicationContext;

	/**
	 * csrf Guard.
	 *
	 * @param properties the properties
	 * @return the result
	 */
	@Bean
	public CsrfGuard csrfGuard(ShiroCsrfguardProperties properties){
		try {
			CsrfGuard.load(properties.toProperties());
		} catch (Exception e) {
		}
		return CsrfGuard.getInstance();
	}

	/**
	 * java Script Servlet.
	 *
	 * @param properties the properties
	 * @return the result
	 * @throws Exception if an error occurs
	 */
	@Bean
    @ConditionalOnMissingBean
	public ServletRegistrationBean<JavaScriptServlet> javaScriptServlet(ShiroCsrfguardProperties properties) throws Exception {

		JavaScriptServlet javaScriptServlet = new JavaScriptServlet();
		ServletRegistrationBean<JavaScriptServlet> registrationBean =
				new ServletRegistrationBean<>(javaScriptServlet);

		// 默认参数
		CsrfguardJavascriptServletProperties javascript = properties.getJavascript();
		registrationBean.addInitParameter(CsrfguardConstants.CACHE_CONTROL_KEY, javascript.getCacheControl());
		registrationBean.addInitParameter(CsrfguardConstants.DOMAIN_STRICT_KEY, Boolean.toString(javascript.isDomainStrict()));
		registrationBean.addInitParameter(CsrfguardConstants.INJECT_FORM_ATTRIBUTES_KEY, Boolean.toString(javascript.isInjectIntoAttributes()));
		registrationBean.addInitParameter(CsrfguardConstants.INJECT_GET_FORMS_KEY, Boolean.toString(javascript.isInjectGetForms()));
		registrationBean.addInitParameter(CsrfguardConstants.INJECT_INTO_ATTRIBUTES_KEY, Boolean.toString(javascript.isInjectFormAttributes()));
		registrationBean.addInitParameter(CsrfguardConstants.INJECT_INTO_FORMS_KEY, Boolean.toString(javascript.isInjectIntoForms()));
		registrationBean.addInitParameter(CsrfguardConstants.REFERER_PATTERN_KEY, javascript.getRefererPattern());
		registrationBean.addInitParameter(CsrfguardConstants.REFERER_MATCH_DOMAIN_KEY, Boolean.toString(javascript.isRefererMatchDomain()));
		registrationBean.addInitParameter(CsrfguardConstants.SOURCE_FILE_KEY, javascript.getSourceFile());
		registrationBean.addInitParameter(CsrfguardConstants.XREQUESTEDWITH_KEY, javascript.getXRequestedWith());
		registrationBean.addUrlMappings(javascript.getPattern());

        return registrationBean;
    }

	/**
	 * csrf Guard HTTP Session Listener.
	 *
	 * @return the result
	 */
	@Bean
	@ConditionalOnProperty(prefix = "shiro", value = "session-creation-enabled", havingValue = "true")
	protected ServletListenerRegistrationBean<CsrfGuardHttpSessionListener> csrfGuardHttpSessionListener()
			throws Exception {

		ServletListenerRegistrationBean<CsrfGuardHttpSessionListener> registration =
				new ServletListenerRegistrationBean<>(new CsrfGuardHttpSessionListener());
		registration.setOrder(Integer.MIN_VALUE);
		registration.setEnabled(false);

		return registration;
	}


    /**
     * csrf Guard Filter.
     *
     * @return the result
     * @throws Exception if an error occurs
     */
	@Bean("csrf")
    @ConditionalOnMissingBean(name = "csrf")
    protected FilterRegistrationBean<CsrfGuardControlFilter> csrfGuardFilter() throws Exception {

        FilterRegistrationBean<CsrfGuardControlFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CsrfGuardControlFilter());
        registration.setOrder(Integer.MIN_VALUE);
        registration.setEnabled(false);
        return registration;

    }

	/**
	 * csrf Guard Servlet Context Listener.
	 *
	 * @return the result
	 */
	@Bean
	protected CsrfGuardServletContextListener csrfGuardServletContextListener() {
		return new CsrfGuardServletContextListener();
	}

	/**
	 * Sets the application context.
	 *
	 * @param applicationContext the application context
	 * @throws BeansException if an error occurs
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * Returns the application context.
	 *
	 * @return the application context
	 */
	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

}
