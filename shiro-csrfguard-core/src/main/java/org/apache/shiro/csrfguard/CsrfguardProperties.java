/*
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.shiro.csrfguard;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import lombok.Data;
import org.apache.commons.collections.MapUtils;
import org.apache.shiro.biz.utils.StringUtils2;

/**
 * TODO
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Data
public class CsrfguardProperties {

	private final static String ACTION_PREFIX = "org.owasp.csrfguard.action.";

	private final static String PROTECTED_PAGE_PREFIX = "org.owasp.csrfguard.protected.";

	private final static String UNPROTECTED_PAGE_PREFIX = "org.owasp.csrfguard.unprotected.";

	public enum LoggerType {

		CONSOLE("org.owasp.csrfguard.log.ConsoleLogger"), 
		JAVA("org.owasp.csrfguard.log.JavaLogger");

		private final String implClassName;

		LoggerType(String implClassName) {
			this.implClassName = implClassName;
		}

		/**
		 * class Name.
		 *
		 * @return the result
		 */
		public String className() {
			return implClassName;
		}

		/**
		 * Determines whether equals.
		 *
		 * @param loggerType the logger type
		 * @return the result
		 */
		public boolean equals(LoggerType loggerType) {
			return this.compareTo(loggerType) == 0;
		}

	}

	private boolean enabled = false;
	private LoggerType logger = LoggerType.CONSOLE;
	private String tokenName = "OWASP_CSRFGUARD";
	private int tokenLength = 32;
	private boolean rotateEnabled = false;
	private boolean tokenPerPageEnabled = false;
	/**
	 * If csrf guard filter should check even if there is no session for the user
	 * Note: this changed in 2014/04, the default behavior used to be to not check
	 * if there is no session. If you want the legacy behavior (if your app is not
	 * susceptible to CSRF if the user has no session), set this to false
	 */
	private boolean validationWhenNoSessionExists = true;

	private boolean tokenPerPagePrecreateEnabled = false;
	private boolean printConfig = false;
	private String prng = "SHA1PRNG";
	private String prngProvider = "SUN";

	private String newTokenLandingPage;

	private boolean useNewTokenLandingPage = false;

	private boolean ajaxEnabled = false;

	private boolean protectEnabled = false;

	private String sessionKey = "OWASP_CSRFGUARD_KEY";

    /** CSRFGuard 4 的会话标识提取实现。 */
    private String logicalSessionExtractor = "org.owasp.csrfguard.session.SessionTokenKeyExtractor";

	private Map<String, String> actions = new HashMap<>(Map.of(
            "Error", "org.owasp.csrfguard.action.Error",
            "Error.Code", "403",
            "Error.Message", "Invalid CSRF token"));

	private Map<String, String> protectedPages = new HashMap<String, String>();

	private Map<String, String> unprotectedPages = new HashMap<String, String>();

	private Set<String> protectedMethods = new HashSet<String>();

	private Set<String> unprotectedMethods = new HashSet<String>();

	/**
	 * to Properties.
	 *
	 * @return the result
	 */
	public Properties toProperties() {

		Properties properties = new Properties();

		putProperty(properties, "org.owasp.csrfguard.Logger", logger.className());
		putProperty(properties, "org.owasp.csrfguard.TokenName", tokenName);
		putProperty(properties, "org.owasp.csrfguard.TokenLength", tokenLength);
		putProperty(properties, "org.owasp.csrfguard.Rotate", rotateEnabled);
		putProperty(properties, "org.owasp.csrfguard.TokenPerPage", tokenPerPageEnabled);
		putProperty(properties, "org.owasp.csrfguard.ValidateWhenNoSessionExists", validationWhenNoSessionExists);
		putProperty(properties, "org.owasp.csrfguard.TokenPerPagePrecreate", tokenPerPagePrecreateEnabled);
		putProperty(properties, "org.owasp.csrfguard.PRNG", prng);
		putProperty(properties, "org.owasp.csrfguard.PRNG.Provider", prngProvider);
		putProperty(properties, "org.owasp.csrfguard.NewTokenLandingPage", newTokenLandingPage);
		putProperty(properties, "org.owasp.csrfguard.Config.Print", printConfig);
		putProperty(properties, "org.owasp.csrfguard.Enabled", enabled);
		putProperty(properties, "org.owasp.csrfguard.UseNewTokenLandingPage", useNewTokenLandingPage);
		putProperty(properties, "org.owasp.csrfguard.SessionKey", sessionKey);
        putProperty(properties, "org.owasp.csrfguard.LogicalSessionExtractor", logicalSessionExtractor);
		putProperty(properties, "org.owasp.csrfguard.Ajax", ajaxEnabled);
		putProperty(properties, "org.owasp.csrfguard.Protect", protectEnabled);
		putProperty(properties, "org.owasp.csrfguard.ProtectedMethods", StringUtils2.join(protectedMethods, ","));
		putProperty(properties, "org.owasp.csrfguard.UnprotectedMethods", StringUtils2.join(unprotectedMethods, ","));
		
		if(MapUtils.isNotEmpty(actions)) {
			Iterator<String> ite = actions.keySet().iterator();
			while (ite.hasNext()) {
				String key = ite.next();
				putProperty(properties, ACTION_PREFIX + key, actions.get(key));
			}
		}

		if(MapUtils.isNotEmpty(protectedPages)) {
			Iterator<String> ite = protectedPages.keySet().iterator();
			while (ite.hasNext()) {
				String key = ite.next();
				putProperty(properties, PROTECTED_PAGE_PREFIX + key, protectedPages.get(key));
			}
		}
		
		if(MapUtils.isNotEmpty(unprotectedPages)) {
			Iterator<String> ite = unprotectedPages.keySet().iterator();
			while (ite.hasNext()) {
				String key = ite.next();
				putProperty(properties, UNPROTECTED_PAGE_PREFIX + key, unprotectedPages.get(key));
			}
		}

		return properties;
	}

    /** 将可选配置转换成 Properties 可读取的字符串，省略未设置的值。 */
    protected static void putProperty(Properties properties, String key, Object value) {
        if (Objects.nonNull(value)) {
            properties.setProperty(key, String.valueOf(value));
        }
    }
}
