package com.innercircle.sacco.security.listener;

import com.innercircle.sacco.common.event.MemberCreatedEvent;
import com.innercircle.sacco.member.entity.Member;
import com.innercircle.sacco.member.repository.MemberRepository;
import com.innercircle.sacco.security.entity.Role;
import com.innercircle.sacco.security.entity.UserAccount;
import com.innercircle.sacco.security.repository.RoleRepository;
import com.innercircle.sacco.security.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberUserProvisioningListener")
class MemberUserProvisioningListenerTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberUserProvisioningListener listener;

    @Test
    @DisplayName("creates MEMBER user account when member has no linked user")
    void createsUserForNewMember() {
        UUID memberId = UUID.randomUUID();
        Member member = createMember(memberId, "MBR 001", "jane.member@example.com");

        Role memberRole = new Role();
        memberRole.setName("MEMBER");

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(roleRepository.findByName("MEMBER")).thenReturn(Optional.of(memberRole));
        when(userAccountRepository.findByMemberId(memberId)).thenReturn(Optional.empty());
        when(userAccountRepository.findByEmail(member.getEmail())).thenReturn(Optional.empty());
        when(userAccountRepository.findByUsername("mbr-001")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

        listener.handleMemberCreated(new MemberCreatedEvent(
                memberId,
                member.getMemberNumber(),
                member.getFirstName(),
                member.getLastName(),
                UUID.randomUUID(),
                "admin"
        ));

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());

        UserAccount saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("mbr-001");
        assertThat(saved.getEmail()).isEqualTo(member.getEmail());
        assertThat(saved.getMemberId()).isEqualTo(memberId);
        assertThat(saved.getRoles()).extracting(Role::getName).containsExactly("MEMBER");
        assertThat(saved.getEnabled()).isTrue();
        assertThat(saved.getAccountNonLocked()).isTrue();
        assertThat(saved.getPassword()).isEqualTo("encoded-password");
    }

    @Test
    @DisplayName("updates existing email-based user with member linkage and MEMBER role")
    void updatesExistingUserByEmail() {
        UUID memberId = UUID.randomUUID();
        Member member = createMember(memberId, "MBR-200", "existing.user@example.com");

        Role memberRole = new Role();
        memberRole.setName("MEMBER");

        Role adminRole = new Role();
        adminRole.setName("ADMIN");

        UserAccount existing = UserAccount.builder()
                .username("existing.user")
                .email(member.getEmail())
                .password("encoded")
                .enabled(true)
                .accountNonLocked(true)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(roleRepository.findByName("MEMBER")).thenReturn(Optional.of(memberRole));
        when(userAccountRepository.findByMemberId(memberId)).thenReturn(Optional.empty());
        when(userAccountRepository.findByEmail(member.getEmail())).thenReturn(Optional.of(existing));

        listener.handleMemberCreated(new MemberCreatedEvent(
                memberId,
                member.getMemberNumber(),
                member.getFirstName(),
                member.getLastName(),
                UUID.randomUUID(),
                "admin"
        ));

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());

        UserAccount updated = captor.getValue();
        assertThat(updated.getMemberId()).isEqualTo(memberId);
        assertThat(updated.getRoles()).extracting(Role::getName)
                .containsExactlyInAnyOrder("ADMIN", "MEMBER");
    }

    @Test
    @DisplayName("adds numeric suffix when username candidate is already taken")
    void addsUsernameSuffixOnCollision() {
        UUID memberId = UUID.randomUUID();
        Member member = createMember(memberId, "MBR-10", "collision@example.com");

        Role memberRole = new Role();
        memberRole.setName("MEMBER");

        UserAccount takenUser = UserAccount.builder()
                .username("mbr-10")
                .email("another@example.com")
                .password("encoded")
                .enabled(true)
                .accountNonLocked(true)
                .roles(new HashSet<>())
                .build();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(roleRepository.findByName("MEMBER")).thenReturn(Optional.of(memberRole));
        when(userAccountRepository.findByMemberId(memberId)).thenReturn(Optional.empty());
        when(userAccountRepository.findByEmail(member.getEmail())).thenReturn(Optional.empty());
        when(userAccountRepository.findByUsername("mbr-10")).thenReturn(Optional.of(takenUser));
        when(userAccountRepository.findByUsername("mbr-10-1")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

        listener.handleMemberCreated(new MemberCreatedEvent(
                memberId,
                member.getMemberNumber(),
                member.getFirstName(),
                member.getLastName(),
                UUID.randomUUID(),
                "admin"
        ));

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("mbr-10-1");
    }

    @Test
    @DisplayName("does nothing when member record cannot be found")
    void doesNothingWhenMemberMissing() {
        UUID missingMemberId = UUID.randomUUID();
        when(memberRepository.findById(missingMemberId)).thenReturn(Optional.empty());

        listener.handleMemberCreated(new MemberCreatedEvent(
                missingMemberId,
                "MBR-404",
                "Missing",
                "Member",
                UUID.randomUUID(),
                "system"
        ));

        verifyNoInteractions(roleRepository);
        verifyNoInteractions(userAccountRepository);
        verifyNoInteractions(passwordEncoder);
    }

    private Member createMember(UUID id, String memberNumber, String email) {
        Member member = new Member();
        member.setId(id);
        member.setMemberNumber(memberNumber);
        member.setFirstName("Jane");
        member.setLastName("Member");
        member.setEmail(email);
        return member;
    }
}
