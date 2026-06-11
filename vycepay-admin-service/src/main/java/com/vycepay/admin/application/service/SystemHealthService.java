package com.vycepay.admin.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.vycepay.admin.config.AdminProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/** Collects sanitized service health information for authorized backoffice users. */
@Service
public class SystemHealthService {
    private final AdminProperties properties; private final RestTemplate restTemplate;
    public SystemHealthService(AdminProperties properties, RestTemplateBuilder builder) { this.properties=properties; this.restTemplate=builder.setConnectTimeout(java.time.Duration.ofSeconds(2)).setReadTimeout(java.time.Duration.ofSeconds(2)).build(); }
    public Map<String,Object> health() { var futures=properties.getHealth().getServices().stream().map(target -> CompletableFuture.supplyAsync(() -> serviceHealth(target))).toList(); var services=new ArrayList<Map<String,Object>>(); futures.forEach(future -> services.add(future.join())); Map<String,Object> choice=new LinkedHashMap<>(); long start=System.currentTimeMillis(); try{ restTemplate.headForHeaders(properties.getHealth().getChoiceBankUrl()); choice.put("reachable", true); }catch(Exception e){ choice.put("reachable", false); } choice.put("latencyMs", System.currentTimeMillis()-start); return Map.of("services", services, "choiceBank", choice); }
    private Map<String,Object> serviceHealth(AdminProperties.ServiceTarget target){ long start=System.currentTimeMillis(); Map<String,Object> item=new LinkedHashMap<>(); item.put("name", target.getName()); item.put("lastChecked", Instant.now().toString()); try{ restTemplate.getForObject(target.getUrl(), String.class); item.put("status","UP"); }catch(Exception e){ item.put("status","DOWN"); item.put("detail","Health endpoint unavailable"); } item.put("responseTimeMs", System.currentTimeMillis()-start); return item; }
}
