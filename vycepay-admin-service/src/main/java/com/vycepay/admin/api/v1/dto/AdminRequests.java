package com.vycepay.admin.api.v1.dto;

import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request DTOs for admin authentication and mutation APIs. */
public final class AdminRequests {
    private static final Set<String> CUSTOMER_STATUSES = Set.of("ACTIVE", "SUSPENDED", "DEACTIVATED");
    private static final Set<String> WALLET_STATUSES = Set.of("ACTIVE", "FROZEN", "SUSPENDED", "CLOSED");
    private static final Set<String> ADMIN_USER_STATUSES = Set.of("ACTIVE", "SUSPENDED");
    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,128}$";

    private AdminRequests() {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 255) String username,
            @NotBlank @Size(max = 128) String password) {
    }

    public record MfaRequest(
            @NotBlank @Size(max = 64) String jti,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "totpCode must be a 6 digit code") String totpCode) {
    }

    public record ForgotPasswordRequest(@NotBlank @Email @Size(max = 255) String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Size(min = 32, max = 128) String token,
            @NotBlank @Pattern(regexp = PASSWORD_PATTERN, message = "newPassword must be 12-128 characters and include upper, lower, number, and symbol") String newPassword) {
    }

    public record CustomerStatusRequest(
            @NotBlank String status,
            @NotBlank @Size(min = 10, max = 512) String reason) {
        @AssertTrue(message = "status must be ACTIVE, SUSPENDED, or DEACTIVATED")
        public boolean isStatusAllowed() {
            return status != null && CUSTOMER_STATUSES.contains(status);
        }
    }

    public record WalletStatusRequest(
            @NotBlank String status,
            @NotBlank @Size(min = 10, max = 512) String reason) {
        @AssertTrue(message = "status must be ACTIVE, FROZEN, SUSPENDED, or CLOSED")
        public boolean isStatusAllowed() {
            return status != null && WALLET_STATUSES.contains(status);
        }
    }

    public record CallbackRetryRequest(@NotBlank @Size(min = 10, max = 512) String reason) {
    }

    public record MenuRequest(
            @NotBlank @Size(max = 64) String name,
            @NotBlank @Size(max = 128) String route,
            @Size(max = 64) String icon,
            Long parentId,
            Integer sortOrder) {
    }

    public record RoleRequest(
            @NotBlank @Size(max = 64) String name,
            @Size(max = 256) String description,
            @NotNull List<@NotNull Long> menuIds,
            @NotNull List<@NotBlank String> permissionCodes,
            @NotBlank @Size(min = 10, max = 512) String reason) {
    }

    public record AdminUserCreateRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 128) String fullName,
            @NotBlank @Pattern(regexp = PASSWORD_PATTERN, message = "password must be 12-128 characters and include upper, lower, number, and symbol") String password,
            @NotEmpty List<@NotNull Long> roleIds,
            @NotBlank @Size(min = 10, max = 512) String reason) {
    }

    public record AdminUserUpdateRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 128) String fullName,
            @NotBlank String status,
            @NotEmpty List<@NotNull Long> roleIds,
            @NotBlank @Size(min = 10, max = 512) String reason) {
        @AssertTrue(message = "status must be ACTIVE or SUSPENDED")
        public boolean isStatusAllowed() {
            return status != null && ADMIN_USER_STATUSES.contains(status);
        }
    }

    public record AdminPasswordResetRequest(
            @NotBlank @Pattern(regexp = PASSWORD_PATTERN, message = "newPassword must be 12-128 characters and include upper, lower, number, and symbol") String newPassword,
            @NotBlank @Size(min = 10, max = 512) String reason) {
    }
}
