package com.innercircle.sacco.security.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.member.entity.Member;
import com.innercircle.sacco.member.entity.MemberStatus;
import com.innercircle.sacco.member.repository.MemberRepository;
import com.innercircle.sacco.security.dto.MeResponse;
import com.innercircle.sacco.security.entity.Role;
import com.innercircle.sacco.security.entity.UserAccount;
import com.innercircle.sacco.security.repository.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MeController")
class MeControllerTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MeController meController;

    @BeforeEach
    void setUp() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("testuser", null, Collections.emptyList());
        SecurityContext context = new SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserAccount createUserAccount(UUID id, String username, UUID memberId, String... roleNames) {
        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = new Role();
            role.setId(UUID.randomUUID());
            role.setName(roleName);
            roles.add(role);
        }

        UserAccount user = new UserAccount();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("encodedPassword");
        user.setEmail(username + "@example.com");
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setMemberId(memberId);
        user.setRoles(roles);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }

    private Member createMember(UUID id, String memberNumber, String firstName, String lastName) {
        Member member = new Member();
        member.setId(id);
        member.setMemberNumber(memberNumber);
        member.setFirstName(firstName);
        member.setLastName(lastName);
        member.setEmail(firstName.toLowerCase() + "@example.com");
        member.setPhone("0712345678");
        member.setNationalId("12345678");
        member.setDateOfBirth(LocalDate.of(1990, 1, 1));
        member.setJoinDate(LocalDate.of(2024, 1, 1));
        member.setStatus(MemberStatus.ACTIVE);
        member.setShareBalance(BigDecimal.ZERO);
        return member;
    }

    @Nested
    @DisplayName("getCurrentUser")
    class GetCurrentUser {

        @Test
        @DisplayName("should return user with member info when memberId is set and member exists")
        void shouldReturnUserWithMemberInfo() {
            UUID userId = UUID.randomUUID();
            UUID memberId = UUID.randomUUID();
            UserAccount user = createUserAccount(userId, "testuser", memberId, "MEMBER");
            Member member = createMember(memberId, "MEM-001", "John", "Doe");

            when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

            ResponseEntity<ApiResponse<MeResponse>> response = meController.getCurrentUser();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();

            MeResponse meResponse = response.getBody().getData();
            assertThat(meResponse.id()).isEqualTo(userId);
            assertThat(meResponse.username()).isEqualTo("testuser");
            assertThat(meResponse.email()).isEqualTo("testuser@example.com");
            assertThat(meResponse.enabled()).isTrue();
            assertThat(meResponse.roles()).containsExactly("MEMBER");
            assertThat(meResponse.member()).isNotNull();
            assertThat(meResponse.member().id()).isEqualTo(memberId);
            assertThat(meResponse.member().firstName()).isEqualTo("John");
            assertThat(meResponse.member().lastName()).isEqualTo("Doe");
            assertThat(meResponse.member().memberNumber()).isEqualTo("MEM-001");
        }

        @Test
        @DisplayName("should return user without member info when memberId is null")
        void shouldReturnUserWithoutMemberWhenMemberIdNull() {
            UUID userId = UUID.randomUUID();
            UserAccount user = createUserAccount(userId, "testuser", null, "ADMIN");

            when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            ResponseEntity<ApiResponse<MeResponse>> response = meController.getCurrentUser();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            MeResponse meResponse = response.getBody().getData();
            assertThat(meResponse.username()).isEqualTo("testuser");
            assertThat(meResponse.member()).isNull();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user is not found")
        void shouldThrowWhenUserNotFound() {
            when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> meController.getCurrentUser())
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
