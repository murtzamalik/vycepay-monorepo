package com.vycepay.admin.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Externalized admin service security, export, bootstrap, and health settings. */
@ConfigurationProperties(prefix = "vycepay.admin")
public class AdminProperties {
    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Login login = new Login();
    private Export export = new Export();
    private RateLimit rateLimit = new RateLimit();
    private Bootstrap bootstrap = new Bootstrap();
    private Health health = new Health();
    public Jwt getJwt() { return jwt; } public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public Cors getCors() { return cors; } public void setCors(Cors cors) { this.cors = cors; }
    public Login getLogin() { return login; } public void setLogin(Login login) { this.login = login; }
    public Export getExport() { return export; } public void setExport(Export export) { this.export = export; }
    public RateLimit getRateLimit() { return rateLimit; } public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }
    public Bootstrap getBootstrap() { return bootstrap; } public void setBootstrap(Bootstrap bootstrap) { this.bootstrap = bootstrap; }
    public Health getHealth() { return health; } public void setHealth(Health health) { this.health = health; }
    public static class Jwt { private String secret; private long expirationMs = 900000; public String getSecret() { return secret; } public void setSecret(String secret) { this.secret = secret; } public long getExpirationMs() { return expirationMs; } public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; } }
    public static class Cors { private String allowedOrigins = "http://localhost:3000"; public String getAllowedOrigins() { return allowedOrigins; } public void setAllowedOrigins(String allowedOrigins) { this.allowedOrigins = allowedOrigins; } }
    public static class Login { private int maxAttempts = 5; private int lockoutMinutes = 15; public int getMaxAttempts() { return maxAttempts; } public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; } public int getLockoutMinutes() { return lockoutMinutes; } public void setLockoutMinutes(int lockoutMinutes) { this.lockoutMinutes = lockoutMinutes; } }
    public static class Export { private int maxRows = 10000; public int getMaxRows() { return maxRows; } public void setMaxRows(int maxRows) { this.maxRows = maxRows; } }
    public static class RateLimit {
        private Rule login = new Rule(5, 60);
        private Rule reset = new Rule(3, 300);
        private Rule export = new Rule(5, 300);
        private Rule mutation = new Rule(30, 60);
        public Rule getLogin() { return login; } public void setLogin(Rule login) { this.login = login; }
        public Rule getReset() { return reset; } public void setReset(Rule reset) { this.reset = reset; }
        public Rule getExport() { return export; } public void setExport(Rule export) { this.export = export; }
        public Rule getMutation() { return mutation; } public void setMutation(Rule mutation) { this.mutation = mutation; }
        public Rule rule(String name) { return switch (name) { case "login" -> login; case "reset" -> reset; case "export" -> export; case "mutation" -> mutation; default -> null; }; }
        public static class Rule {
            private boolean enabled = true;
            private int limit;
            private int windowSeconds;
            public Rule() { }
            public Rule(int limit, int windowSeconds) { this.limit = limit; this.windowSeconds = windowSeconds; }
            public boolean isEnabled() { return enabled; } public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public int getLimit() { return limit; } public void setLimit(int limit) { this.limit = limit; }
            public int getWindowSeconds() { return windowSeconds; } public void setWindowSeconds(int windowSeconds) { this.windowSeconds = windowSeconds; }
        }
    }
    public static class Bootstrap { private String username; private String email; private String password; private String fullName = "Super Admin"; public String getUsername() { return username; } public void setUsername(String username) { this.username = username; } public String getEmail() { return email; } public void setEmail(String email) { this.email = email; } public String getPassword() { return password; } public void setPassword(String password) { this.password = password; } public String getFullName() { return fullName; } public void setFullName(String fullName) { this.fullName = fullName; } }
    public static class Health { private List<ServiceTarget> services = new ArrayList<>(); private String choiceBankUrl; public List<ServiceTarget> getServices() { return services; } public void setServices(List<ServiceTarget> services) { this.services = services; } public String getChoiceBankUrl() { return choiceBankUrl; } public void setChoiceBankUrl(String choiceBankUrl) { this.choiceBankUrl = choiceBankUrl; } }
    public static class ServiceTarget { private String name; private String url; public String getName() { return name; } public void setName(String name) { this.name = name; } public String getUrl() { return url; } public void setUrl(String url) { this.url = url; } }
}
