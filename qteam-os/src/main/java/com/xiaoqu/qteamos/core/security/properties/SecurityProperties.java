/*
 * Copyright (c) 2023-2025 XiaoQuTeam. All rights reserved.
 * QTeamOS is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

/**
 * Spring Security属性配置类
 * 映射application.yml中的plugin.security配置
 *
 * @author yangqijun
 * @date 2025-07-24
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.security.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全属性配置类
 * 用于从配置文件中读取Spring Security相关配置
 */
@Component
@ConfigurationProperties(prefix = "plugin.security")
public class SecurityProperties {

    private Paths paths = new Paths();
    private Csrf csrf = new Csrf();
    private Session session = new Session();
    private Authentication authentication = new Authentication();
    private Cors cors = new Cors();

    /**
     * 路径权限配置
     */
    public static class Paths {
        private List<String> permitAll = new ArrayList<>();
        private List<String> adminOnly = new ArrayList<>();
        private List<String> authenticated = new ArrayList<>();

        public List<String> getPermitAll() {
            return permitAll;
        }

        public void setPermitAll(List<String> permitAll) {
            this.permitAll = permitAll;
        }

        public List<String> getAdminOnly() {
            return adminOnly;
        }

        public void setAdminOnly(List<String> adminOnly) {
            this.adminOnly = adminOnly;
        }

        public List<String> getAuthenticated() {
            return authenticated;
        }

        public void setAuthenticated(List<String> authenticated) {
            this.authenticated = authenticated;
        }
    }

    /**
     * CSRF保护配置
     */
    public static class Csrf {
        private boolean enabled = false;
        private List<String> excludedPaths = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getExcludedPaths() {
            return excludedPaths;
        }

        public void setExcludedPaths(List<String> excludedPaths) {
            this.excludedPaths = excludedPaths;
        }
    }

    /**
     * 会话管理配置
     */
    public static class Session {
        private String creationPolicy = "STATELESS";

        public String getCreationPolicy() {
            return creationPolicy;
        }

        public void setCreationPolicy(String creationPolicy) {
            this.creationPolicy = creationPolicy;
        }
    }

    /**
     * 认证配置
     */
    public static class Authentication {
        private boolean formLogin = false;
        private boolean httpBasic = false;
        private Jwt jwt = new Jwt();

        public boolean isFormLogin() {
            return formLogin;
        }

        public void setFormLogin(boolean formLogin) {
            this.formLogin = formLogin;
        }

        public boolean isHttpBasic() {
            return httpBasic;
        }

        public void setHttpBasic(boolean httpBasic) {
            this.httpBasic = httpBasic;
        }

        public Jwt getJwt() {
            return jwt;
        }

        public void setJwt(Jwt jwt) {
            this.jwt = jwt;
        }

        /**
         * JWT配置
         */
        public static class Jwt {
            private boolean enabled = true;
            private int filterOrder = 0;
            private String tokenHeader = "Authorization";
            private String tokenPrefix = "Bearer ";
            private boolean ignoreExpiredToken = false;
            private String requestMatcher = "/api/**";

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public int getFilterOrder() {
                return filterOrder;
            }

            public void setFilterOrder(int filterOrder) {
                this.filterOrder = filterOrder;
            }

            public String getTokenHeader() {
                return tokenHeader;
            }

            public void setTokenHeader(String tokenHeader) {
                this.tokenHeader = tokenHeader;
            }

            public String getTokenPrefix() {
                return tokenPrefix;
            }

            public void setTokenPrefix(String tokenPrefix) {
                this.tokenPrefix = tokenPrefix;
            }

            public boolean isIgnoreExpiredToken() {
                return ignoreExpiredToken;
            }

            public void setIgnoreExpiredToken(boolean ignoreExpiredToken) {
                this.ignoreExpiredToken = ignoreExpiredToken;
            }

            public String getRequestMatcher() {
                return requestMatcher;
            }

            public void setRequestMatcher(String requestMatcher) {
                this.requestMatcher = requestMatcher;
            }
        }
    }

    /**
     * CORS配置
     */
    public static class Cors {
        private boolean enabled = true;
        private boolean allowCredentials = true;
        private String allowedHeaders = "*";
        private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";
        private String allowedOrigins = "*";
        private String exposedHeaders = "Authorization";
        private int maxAge = 1800;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public String getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(String allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public String getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(String allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public String getExposedHeaders() {
            return exposedHeaders;
        }

        public void setExposedHeaders(String exposedHeaders) {
            this.exposedHeaders = exposedHeaders;
        }

        public int getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(int maxAge) {
            this.maxAge = maxAge;
        }
    }

    // Getters and Setters

    public Paths getPaths() {
        return paths;
    }

    public void setPaths(Paths paths) {
        this.paths = paths;
    }

    public Csrf getCsrf() {
        return csrf;
    }

    public void setCsrf(Csrf csrf) {
        this.csrf = csrf;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }
} 