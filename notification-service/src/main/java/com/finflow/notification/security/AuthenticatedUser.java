package com.finflow.notification.security;

import java.util.List;

public record AuthenticatedUser(Long userId, String email, List<Long> orgIds, List<String> permissions) {
}
