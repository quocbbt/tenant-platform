package com.tenantcore.common.context;

import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class UserContext {

    private static final ThreadLocal<CurrentUser> USER_HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setCurrentUser(CurrentUser user) {
        USER_HOLDER.set(user);
    }

    public static CurrentUser getCurrentUser() {
        return USER_HOLDER.get();
    }

    public static CurrentUser requireCurrentUser() {
        CurrentUser user = getCurrentUser();

        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return user;
    }

    public static UUID getUserId() {
        CurrentUser user = getCurrentUser();
        return user == null ? null : user.userId();
    }

    public static UUID requireUserId() {
        return requireCurrentUser().userId();
    }

    public static String getUsername() {
        CurrentUser user = getCurrentUser();
        return user == null ? null : user.username();
    }

    public static boolean hasRole(String roleCode) {
        CurrentUser user = getCurrentUser();

        if (user == null || user.roles() == null) {
            return false;
        }

        return user.roles().contains(roleCode);
    }

    public static boolean hasPermission(String permissionCode) {
        CurrentUser user = getCurrentUser();

        if (user == null || user.permissions() == null) {
            return false;
        }

        return user.permissions().contains(permissionCode);
    }

    public static void clear() {
        USER_HOLDER.remove();
    }

    public record CurrentUser(
            UUID userId,
            String tenantCode,
            String username,
            String fullName,
            Set<String> roles,
            Set<String> permissions
    ) {
        public CurrentUser {
            roles = roles == null ? Collections.emptySet() : Set.copyOf(roles);
            permissions = permissions == null ? Collections.emptySet() : Set.copyOf(permissions);
        }

        public List<String> roleList() {
            return roles.stream().toList();
        }

        public List<String> permissionList() {
            return permissions.stream().toList();
        }
    }
}