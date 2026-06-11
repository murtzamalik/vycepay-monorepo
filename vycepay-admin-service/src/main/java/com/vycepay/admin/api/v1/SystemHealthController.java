package com.vycepay.admin.api.v1;
import java.util.*; import com.vycepay.admin.application.service.SystemHealthService; import com.vycepay.common.api.*; import org.springframework.http.ResponseEntity; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
/** Sanitized service and provider health API for operations users. */
@RestController @RequestMapping("/api/admin/v1/system-health") @PreAuthorize("hasAuthority('PERM_system:health')")
public class SystemHealthController{private final SystemHealthService s; public SystemHealthController(SystemHealthService s){this.s=s;} @GetMapping public ResponseEntity<ApiSuccessResponse<Map<String,Object>>> health(){return ResponseEntity.ok(ApiSuccessResponses.ok("SYSTEM_HEALTH_OK","System health",s.health()));}}
