package com.innercircle.sacco.security.listener;

import com.innercircle.sacco.common.event.MemberCreatedEvent;
import com.innercircle.sacco.member.entity.Member;
import com.innercircle.sacco.member.repository.MemberRepository;
import com.innercircle.sacco.security.entity.Role;
import com.innercircle.sacco.security.entity.UserAccount;
import com.innercircle.sacco.security.repository.RoleRepository;
import com.innercircle.sacco.security.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberUserProvisioningListener {

    private static final String MEMBER_ROLE = "MEMBER";

    private final MemberRepository memberRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener
    @Transactional
    public void handleMemberCreated(MemberCreatedEvent event) {
        Member member = memberRepository.findById(event.memberId()).orElse(null);
        if (member == null) {
            log.warn("Skipping user provisioning: member {} not found", event.memberId());
            return;
        }

        Role memberRole = roleRepository.findByName(MEMBER_ROLE)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + MEMBER_ROLE));

        UserAccount user = userAccountRepository.findByMemberId(member.getId())
                .orElseGet(() -> userAccountRepository.findByEmail(member.getEmail()).orElse(null));

        if (user == null) {
            createMemberUser(member, memberRole);
            return;
        }

        updateExistingUser(member, user, memberRole);
    }

    private void createMemberUser(Member member, Role memberRole) {
        String username = resolveUniqueUsername(member.getMemberNumber());

        UserAccount user = UserAccount.builder()
                .username(username)
                .email(member.getEmail())
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .enabled(true)
                .accountNonLocked(true)
                .memberId(member.getId())
                .roles(new HashSet<>(Set.of(memberRole)))
                .build();

        userAccountRepository.save(user);

        log.info("Provisioned MEMBER user '{}' for member {} ({})",
                user.getUsername(), member.getMemberNumber(), member.getId());
    }

    private void updateExistingUser(Member member, UserAccount user, Role memberRole) {
        boolean changed = false;

        if (user.getMemberId() == null) {
            user.setMemberId(member.getId());
            changed = true;
        } else if (!user.getMemberId().equals(member.getId())) {
            log.warn(
                    "Skipping member link update for user '{}' due to conflicting memberId (existing={}, incoming={})",
                    user.getUsername(),
                    user.getMemberId(),
                    member.getId()
            );
            return;
        }

        if (ensureRole(user, memberRole)) {
            changed = true;
        }

        if (changed) {
            userAccountRepository.save(user);
            log.info("Updated existing user '{}' with MEMBER role/member linkage for member {}",
                    user.getUsername(), member.getMemberNumber());
        }
    }

    private boolean ensureRole(UserAccount user, Role role) {
        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }

        boolean hasRole = user.getRoles().stream()
                .anyMatch(existing -> role.getName().equals(existing.getName()));

        if (hasRole) {
            return false;
        }

        user.getRoles().add(role);
        return true;
    }

    private String resolveUniqueUsername(String rawCandidate) {
        String base = sanitizeUsername(rawCandidate);
        if (base.isBlank()) {
            base = "member";
        }

        String candidate = base;
        int suffix = 1;

        while (userAccountRepository.findByUsername(candidate).isPresent()) {
            String withSuffix = base + "-" + suffix;
            candidate = withSuffix.length() > 100 ? withSuffix.substring(0, 100) : withSuffix;
            suffix++;
        }

        return candidate;
    }

    private String sanitizeUsername(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim().toLowerCase();
        normalized = normalized.replaceAll("[^a-z0-9._-]", "-");
        normalized = normalized.replaceAll("-+", "-");
        normalized = normalized.replaceAll("^-", "");
        normalized = normalized.replaceAll("-$", "");

        if (normalized.length() > 100) {
            normalized = normalized.substring(0, 100);
        }

        return normalized;
    }
}
