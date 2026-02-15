package com.innercircle.sacco.security.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.security.dto.UserResponse;
import com.innercircle.sacco.security.entity.Role;
import com.innercircle.sacco.security.entity.UserAccount;
import com.innercircle.sacco.security.repository.RoleRepository;
import com.innercircle.sacco.security.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public UserResponse activateUser(UUID userId) {
        UserAccount user = findUserById(userId);
        user.setEnabled(true);
        UserAccount saved = userAccountRepository.save(user);
        return mapToUserResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse deactivateUser(UUID userId) {
        preventSelfModification(userId, "deactivate");
        UserAccount user = findUserById(userId);
        user.setEnabled(false);
        UserAccount saved = userAccountRepository.save(user);
        return mapToUserResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse lockUser(UUID userId) {
        preventSelfModification(userId, "lock");
        UserAccount user = findUserById(userId);
        user.setAccountNonLocked(false);
        UserAccount saved = userAccountRepository.save(user);
        return mapToUserResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse unlockUser(UUID userId) {
        UserAccount user = findUserById(userId);
        user.setAccountNonLocked(true);
        UserAccount saved = userAccountRepository.save(user);
        return mapToUserResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<UserResponse> searchUsers(String query, String cursor, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<UserAccount> users;

        if (query == null || query.isBlank()) {
            return listUsers(cursor, limit);
        }

        users = userAccountRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                query, query, pageRequest);

        // Apply cursor filtering if provided
        if (cursor != null && !cursor.isEmpty()) {
            UUID cursorId = UUID.fromString(cursor);
            users = users.stream()
                    .filter(user -> user.getId().compareTo(cursorId) > 0)
                    .toList();
        }

        return buildCursorPage(users, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        UserAccount user = findUserById(userId);
        return mapToUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<UserResponse> listUsers(String cursor, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<UserAccount> users;

        if (cursor != null && !cursor.isEmpty()) {
            UUID cursorId = UUID.fromString(cursor);
            users = userAccountRepository.findByIdGreaterThanOrderById(cursorId, pageRequest);
        } else {
            users = userAccountRepository.findAll(pageRequest).getContent();
        }

        return buildCursorPage(users, limit);
    }

    @Override
    @Transactional
    public UserResponse updateUserRoles(UUID userId, Set<String> roleNames) {
        UserAccount user = findUserById(userId);

        if (roleNames == null || roleNames.isEmpty()) {
            throw new BusinessException("At least one role must be specified");
        }

        // Prevent admin from removing their own ADMIN role
        String currentUsername = getCurrentUsername();
        if (user.getUsername().equals(currentUsername)) {
            boolean currentlyAdmin = user.getRoles().stream()
                    .anyMatch(r -> "ADMIN".equals(r.getName()));
            boolean newRolesContainAdmin = roleNames.contains("ADMIN");
            if (currentlyAdmin && !newRolesContainAdmin) {
                throw new BusinessException("Cannot remove ADMIN role from your own account");
            }
        }

        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", roleName));
            roles.add(role);
        }

        user.setRoles(roles);
        UserAccount saved = userAccountRepository.save(user);
        return mapToUserResponse(saved);
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        preventSelfModification(userId, "delete");
        UserAccount user = findUserById(userId);
        user.setEnabled(false);
        userAccountRepository.save(user);
    }

    private void preventSelfModification(UUID targetUserId, String action) {
        String currentUsername = getCurrentUsername();
        if (currentUsername != null) {
            userAccountRepository.findByUsername(currentUsername).ifPresent(currentUser -> {
                if (currentUser.getId().equals(targetUserId)) {
                    throw new BusinessException("Cannot " + action + " your own account");
                }
            });
        }
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    private UserAccount findUserById(UUID userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private boolean matchesSearchQuery(UserAccount user, String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        String lowerQuery = query.toLowerCase();
        return user.getUsername().toLowerCase().contains(lowerQuery) ||
                user.getEmail().toLowerCase().contains(lowerQuery);
    }

    private CursorPage<UserResponse> buildCursorPage(List<UserAccount> users, int limit) {
        boolean hasMore = users.size() > limit;
        List<UserAccount> pageUsers = hasMore ? users.subList(0, limit) : users;

        List<UserResponse> userResponses = pageUsers.stream()
                .map(this::mapToUserResponse)
                .toList();

        String nextCursor = hasMore ? pageUsers.get(pageUsers.size() - 1).getId().toString() : null;

        return CursorPage.of(userResponses, nextCursor, hasMore);
    }

    private UserResponse mapToUserResponse(UserAccount user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .enabled(user.getEnabled())
                .accountNonLocked(user.getAccountNonLocked())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
