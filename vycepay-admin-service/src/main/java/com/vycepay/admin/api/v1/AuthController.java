package com.vycepay.admin.api.v1;

import java.util.Map;

import com.vycepay.admin.api.v1.dto.AdminRequests.ForgotPasswordRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.LoginRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.MfaRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.ResetPasswordRequest;
import com.vycepay.admin.application.service.AdminAuthService;
import com.vycepay.admin.application.service.RateLimitService;
import com.vycepay.common.api.ApiSuccessResponse;
import com.vycepay.common.api.ApiSuccessResponses;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Admin authentication endpoints for login, MFA, logout, and password reset. */
@RestController @RequestMapping("/api/admin/v1/auth")
public class AuthController { private final AdminAuthService service; private final RateLimitService rateLimitService; public AuthController(AdminAuthService service, RateLimitService rateLimitService){this.service=service;this.rateLimitService=rateLimitService;}
@PostMapping("/login") public ResponseEntity<ApiSuccessResponse<Map<String,Object>>> login(@Valid @RequestBody LoginRequest body, HttpServletRequest req){ rateLimitService.check("login", req); Map<String,Object> data=service.login(body, req); return ResponseEntity.ok(ApiSuccessResponses.ok("ADMIN_LOGIN_OK","Login processed",data)); }
@PostMapping("/login/mfa") public ResponseEntity<ApiSuccessResponse<Map<String,Object>>> mfa(@Valid @RequestBody MfaRequest body, HttpServletRequest req){ rateLimitService.check("login", req); Map<String,Object> data=service.verifyMfa(body); return ResponseEntity.ok(ApiSuccessResponses.ok("ADMIN_MFA_OK","MFA verified",data)); }
@PostMapping("/logout") public ResponseEntity<ApiSuccessResponse<Void>> logout(){ service.logout(); return ResponseEntity.ok(ApiSuccessResponses.ok("ADMIN_LOGOUT_OK","Logged out")); }
@GetMapping("/me") public ResponseEntity<ApiSuccessResponse<Map<String,Object>>> me(){ return ResponseEntity.ok(ApiSuccessResponses.ok("ADMIN_ME_OK","Current admin",service.me())); }
@PostMapping("/forgot-password") public ResponseEntity<ApiSuccessResponse<Map<String,Object>>> forgot(@Valid @RequestBody ForgotPasswordRequest body, HttpServletRequest req){ rateLimitService.check("reset", req); return ResponseEntity.ok(ApiSuccessResponses.ok("ADMIN_RESET_REQUESTED","Password reset accepted",service.forgotPassword(body))); }
@PostMapping("/reset-password") public ResponseEntity<ApiSuccessResponse<Void>> reset(@Valid @RequestBody ResetPasswordRequest body, HttpServletRequest req){ rateLimitService.check("reset", req); service.resetPassword(body); return ResponseEntity.ok(ApiSuccessResponses.ok("ADMIN_PASSWORD_RESET","Password reset complete")); }
}
