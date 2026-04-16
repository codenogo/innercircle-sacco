package com.innercircle.sacco.member.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.event.MemberCreatedEvent;
import com.innercircle.sacco.common.event.MemberStatusChangeEvent;
import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.InvalidStateTransitionException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.member.entity.Member;
import com.innercircle.sacco.member.entity.MemberStatus;
import com.innercircle.sacco.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.innercircle.sacco.common.outbox.EventOutboxWriter;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private EventOutboxWriter outboxWriter;

    @InjectMocks
    private MemberServiceImpl memberService;

    private Member sampleMember;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        sampleMember = new Member(
                "MBR-001",
                "John",
                "Doe",
                "john.doe@example.com",
                "+254712345678",
                "ID12345",
                LocalDate.of(1990, 1, 15),
                LocalDate.of(2024, 1, 1)
        );
        sampleMember.setId(memberId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------
    // create()
    // -------------------------------------------------------
    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("should create member with an auto-generated member number")
        void shouldCreateMemberSuccessfully() {
            when(memberRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
            when(memberRepository.existsByNationalId("ID12345")).thenReturn(false);
            when(memberRepository.existsByMemberNumber(anyString())).thenReturn(false);
            when(memberRepository.save(sampleMember)).thenReturn(sampleMember);

            Member result = memberService.create(sampleMember);

            assertThat(result).isNotNull();
            assertThat(result.getMemberNumber()).startsWith("IC-");
            assertThat(result.getMemberNumber()).hasSize(11);
            assertThat(result.getFirstName()).isEqualTo("John");
            assertThat(result.getLastName()).isEqualTo("Doe");
            assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
            assertThat(result.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(result.getShareBalance()).isEqualTo(BigDecimal.ZERO);
            verify(memberRepository).save(sampleMember);
        }

        @Test
        @DisplayName("should throw BusinessException when member number generation is exhausted")
        void shouldThrowWhenMemberNumberGenerationIsExhausted() {
            when(memberRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
            when(memberRepository.existsByNationalId("ID12345")).thenReturn(false);
            when(memberRepository.existsByMemberNumber(anyString())).thenReturn(true);

            assertThatThrownBy(() -> memberService.create(sampleMember))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Unable to generate unique member number");

            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BusinessException when email already exists")
        void shouldThrowWhenEmailExists() {
            when(memberRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

            assertThatThrownBy(() -> memberService.create(sampleMember))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Email already exists: john.doe@example.com");

            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BusinessException when national ID already exists")
        void shouldThrowWhenNationalIdExists() {
            when(memberRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
            when(memberRepository.existsByNationalId("ID12345")).thenReturn(true);

            assertThatThrownBy(() -> memberService.create(sampleMember))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("National ID already exists: ID12345");

            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("should publish MemberCreatedEvent with authenticated actor")
        void shouldPublishMemberCreatedEvent() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("admin@sacco.co.ke", null, List.of()));

            when(memberRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
            when(memberRepository.existsByNationalId("ID12345")).thenReturn(false);
            when(memberRepository.existsByMemberNumber(anyString())).thenReturn(false);
            when(memberRepository.save(sampleMember)).thenReturn(sampleMember);

            memberService.create(sampleMember);

            ArgumentCaptor<MemberCreatedEvent> eventCaptor = ArgumentCaptor.forClass(MemberCreatedEvent.class);
            verify(outboxWriter).write(eventCaptor.capture(), eq("Member"), any(UUID.class));

            MemberCreatedEvent event = eventCaptor.getValue();
            assertThat(event.memberId()).isEqualTo(memberId);
            assertThat(event.memberNumber()).startsWith("IC-");
            assertThat(event.firstName()).isEqualTo("John");
            assertThat(event.lastName()).isEqualTo("Doe");
            assertThat(event.actor()).isEqualTo("admin@sacco.co.ke");
        }

        @Test
        @DisplayName("should fall back to 'system' actor when no authentication context")
        void shouldFallBackToSystemWhenNoAuth() {
            SecurityContextHolder.clearContext();

            when(memberRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
            when(memberRepository.existsByNationalId("ID12345")).thenReturn(false);
            when(memberRepository.existsByMemberNumber(anyString())).thenReturn(false);
            when(memberRepository.save(sampleMember)).thenReturn(sampleMember);

            memberService.create(sampleMember);

            ArgumentCaptor<MemberCreatedEvent> eventCaptor = ArgumentCaptor.forClass(MemberCreatedEvent.class);
            verify(outboxWriter).write(eventCaptor.capture(), eq("Member"), any(UUID.class));

            assertThat(eventCaptor.getValue().actor()).isEqualTo("system");
        }
    }

    // -------------------------------------------------------
    // update()
    // -------------------------------------------------------
    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("should update all provided fields successfully")
        void shouldUpdateAllFields() {
            Member updateData = new Member();
            updateData.setFirstName("Jane");
            updateData.setLastName("Smith");
            updateData.setEmail("jane.smith@example.com");
            updateData.setPhone("+254700000000");
            updateData.setNationalId("ID99999");
            updateData.setDateOfBirth(LocalDate.of(1985, 5, 20));

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(sampleMember));
            when(memberRepository.existsByEmail("jane.smith@example.com")).thenReturn(false);
            when(memberRepository.existsByNationalId("ID99999")).thenReturn(false);
            when(memberRepository.save(sampleMember)).thenReturn(sampleMember);

            Member result = memberService.update(memberId, updateData);

            assertThat(result.getFirstName()).isEqualTo("Jane");
            assertThat(result.getLastName()).isEqualTo("Smith");
            assertThat(result.getEmail()).isEqualTo("jane.smith@example.com");
            assertThat(result.getPhone()).isEqualTo("+254700000000");
            assertThat(result.getNationalId()).isEqualTo("ID99999");
            assertThat(result.getDateOfBirth()).isEqualTo(LocalDate.of(1985, 5, 20));
            verify(memberRepository).save(sampleMember);
        }

        @Test
        @DisplayName("should update only firstName when only firstName is provided")
        void shouldUpdateOnlyFirstName() {
            Member updateData = new Member();
            updateData.setFirstName("Jane");

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(sampleMember));
            when(memberRepository.save(sampleMember)).thenReturn(sampleMember);

            Member result = memberService.update(memberId, updateData);

            assertThat(result.getFirstName()).isEqualTo("Jane");
            assertThat(result.getLastName()).isEqualTo("Doe");
            assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
            verify(memberRepository).save(sampleMember);
        }

        @Test
        @DisplayName("should update only lastName when only lastName is provided")
        void shouldUpdateOnlyLastName() {
            Member updateData = new Member();
            updateData.setLastName("Smith");

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(sampleMember));
            when(memberRepository.save(sampleMember)).thenReturn(sampleMember);

            Member result = memberService.update(memberId, updateData);

            assertThat(result.getLastName()).isEqualTo("Smith");
            assertThat(result.getFirstName()).isEqualTo("John");
        }

        @Test
        @DisplayName("should update only phone when only phone is provided")
        void shouldUpdateOnlyPhone() {
            Member updateData = new Member();
            updateData.setPhone("+254711111111");

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(sampleMember));
            when(memberRepository.save(sampleMember)).thenReturn(sampleMember);

            Member result = memberService.update(memberId, updateData);

            assertThat(result.getPhone()).isEqualTo("+254711111111");
        }

        @Test
        @DisplayName("should update only dateOfBirth when only dateOfBirth is provided")
        void shouldUpdateOnlyDateOfBirth() {
            Member updateData = new Member();
            updateData.setDateOfBirth(LocalDate.of(1992, 3, 10));

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(sampleMember));
            when(memberRepository.save(sampleMember)).thenReturn(sampleMember);

            Member result = memberService.update(memberId, updateData);

            assertThat(result.getDateOfBirth()).isEqualTo(LocalDate.of(1992, 3, 10));
        }

        @Test
        @DisplayName("should allow same email without uniqueness check")
        void shouldAllowSameEmail() {
            Member updateData = new Member();
            updateData.setEmail("john.doe@example.com"); // same email

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(sampleMember));
            when(memberRepository.save(sampleMember)).thenReturn(sampleMember);

            Member result = memberService.update(memberId, updateData);

            assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
            verify(memberRepository, never()).existsByEmail(any());
        }

        @Test
        @DisplayName("should throw BusinessException when updated email already exists for different member")
        void shouldThrowWhenUpdatedEmailAlreadyExists() {
            Member updateData = new Member();
            updateData.setEmail("existing@example.com");

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(sampleMember));
            when(memberRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> memberService.update(memberId, updateData))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Email already exists: existing@example.com");

            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("should allow same nationalId without uniqueness check")
        void shouldAllowSameNationalId() {
            Member updateData = new Member();
            updateData.setNationalId("ID12345"); // same national ID

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(sampleMember));
            when(memberRepository.save(sampleMember)).thenReturn(sampleMember);

            Member result = memberService.update(memberId, updateData);

            assertThat(result.getNationalId()).isEqualTo("ID12345");
            verify(memberRepository, never()).existsByNationalId(any());
        }

        @Test
        @DisplayName("should throw BusinessException when updated national ID already exists for different member")
        void shouldThrowWhenUpdatedNationalIdAlreadyExists() {
            Member updateData = new Member();
            updateData.setNationalId("ID99999");

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(sampleMember));
            when(memberRepository.existsByNationalId("ID99999")).thenReturn(true);

            assertThatThrownBy(() -> memberService.update(memberId, updateData))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("National ID already exists: ID99999");

            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when member not found")
        void shouldThrowWhenMemberNotFound() {
            UUID unknownId = UUID.randomUUID();
            Member updateData = new Member();
            updateData.setFirstName("Jane");

            when(memberRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> memberService.update(unknownId, updateData))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should not modify any fields when all update fields are null")
        void shouldNotModifyWhenAllFieldsNull() {
            Member updateData = new Member(); // all fields null

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(sampleMember));
            when(memberRepository.save(sampleMember)).thenReturn(sampleMember);

            Member result = memberService.update(memberId, updateData);

            assertThat(result.getFirstName()).isEqualTo("John");
            assertThat(result.getLastName()).isEqualTo("Doe");
            assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
            assertThat(result.getPhone()).isEqualTo("+254712345678");
            assertThat(result.getNationalId()).isEqualTo("ID12345");
        }
    }

    // -------------------------------------------------------
    // findById()
    // -------------------------------------------------------
    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("should return member when found")
        void shouldReturnMemberWhenFound() {
            when(memberRepository.findById(memberId)).thenReturn(Optional.of(sampleMember));

            Member result = memberService.findById(memberId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(memberId);
            assertThat(result.getMemberNumber()).isEqualTo("MBR-001");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when member not found")
        void shouldThrowWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(memberRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> memberService.findById(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Member")
                    .hasMessageContaining(unknownId.toString());
        }
    }

    // -------------------------------------------------------
    // findByMemberNumber()
    // -------------------------------------------------------
    @Nested
    @DisplayName("findByMemberNumber()")
    class FindByMemberNumber {

        @Test
        @DisplayName("should return member when found by member number")
        void shouldReturnMemberWhenFound() {
            when(memberRepository.findByMemberNumber("MBR-001")).thenReturn(Optional.of(sampleMember));

            Member result = memberService.findByMemberNumber("MBR-001");

            assertThat(result).isNotNull();
            assertThat(result.getMemberNumber()).isEqualTo("MBR-001");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when member number not found")
        void shouldThrowWhenNotFound() {
            when(memberRepository.findByMemberNumber("NONEXISTENT")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> memberService.findByMemberNumber("NONEXISTENT"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Member")
                    .hasMessageContaining("NONEXISTENT");
        }
    }

    // -------------------------------------------------------
    // list()
    // -------------------------------------------------------
    @Nested
    @DisplayName("list()")
    class ListMembers {

        @Test
        @DisplayName("should return page with items and hasMore=true when more items exist")
        void shouldReturnPageWithMoreItems() {
            int size = 2;
            List<Member> members = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Member m = new Member();
                m.setId(UUID.randomUUID());
                m.setMemberNumber("MBR-" + i);
                members.add(m);
            }

            when(memberRepository.findByIdGreaterThanOrderById(
                    eq(new UUID(0L, 0L)),
                    eq(PageRequest.of(0, size + 1))
            )).thenReturn(members);

            CursorPage<Member> result = memberService.list(null, size);

            assertThat(result.getItems()).hasSize(size);
            assertThat(result.isHasMore()).isTrue();
            assertThat(result.getNextCursor()).isEqualTo(result.getItems().get(1).getId().toString());
        }

        @Test
        @DisplayName("should return page with hasMore=false when no more items")
        void shouldReturnPageWithNoMoreItems() {
            int size = 5;
            List<Member> members = new ArrayList<>();
            Member m = new Member();
            m.setId(UUID.randomUUID());
            m.setMemberNumber("MBR-0");
            members.add(m);

            when(memberRepository.findByIdGreaterThanOrderById(
                    eq(new UUID(0L, 0L)),
                    eq(PageRequest.of(0, size + 1))
            )).thenReturn(members);

            CursorPage<Member> result = memberService.list(null, size);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.isHasMore()).isFalse();
            assertThat(result.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("should return empty page when no members exist")
        void shouldReturnEmptyPage() {
            int size = 10;
            when(memberRepository.findByIdGreaterThanOrderById(
                    eq(new UUID(0L, 0L)),
                    eq(PageRequest.of(0, size + 1))
            )).thenReturn(new ArrayList<>());

            CursorPage<Member> result = memberService.list(null, size);

            assertThat(result.getItems()).isEmpty();
            assertThat(result.isHasMore()).isFalse();
            assertThat(result.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("should use provided cursor when not null or empty")
        void shouldUseCursorWhenProvided() {
            UUID cursorId = UUID.randomUUID();
            int size = 10;

            when(memberRepository.findByIdGreaterThanOrderById(
                    eq(cursorId),
                    eq(PageRequest.of(0, size + 1))
            )).thenReturn(new ArrayList<>());

            CursorPage<Member> result = memberService.list(cursorId.toString(), size);

            assertThat(result.getItems()).isEmpty();
            verify(memberRepository).findByIdGreaterThanOrderById(eq(cursorId), any());
        }

        @Test
        @DisplayName("should use zero UUID when cursor is empty string")
        void shouldUseZeroUuidWhenCursorIsEmpty() {
            int size = 10;

            when(memberRepository.findByIdGreaterThanOrderById(
                    eq(new UUID(0L, 0L)),
                    eq(PageRequest.of(0, size + 1))
            )).thenReturn(new ArrayList<>());

            CursorPage<Member> result = memberService.list("", size);

            assertThat(result.getItems()).isEmpty();
            verify(memberRepository).findByIdGreaterThanOrderById(eq(new UUID(0L, 0L)), any());
        }

        @Test
        @DisplayName("should return exact size items when returned list equals size + 1")
        void shouldTrimToExactSize() {
            int size = 3;
            List<Member> members = new ArrayList<>();
            for (int i = 0; i < 4; i++) { // size + 1
                Member m = new Member();
                m.setId(UUID.randomUUID());
                members.add(m);
            }

            when(memberRepository.findByIdGreaterThanOrderById(
                    eq(new UUID(0L, 0L)),
                    eq(PageRequest.of(0, size + 1))
            )).thenReturn(members);

            CursorPage<Member> result = memberService.list(null, size);

            assertThat(result.getItems()).hasSize(size);
            assertThat(result.isHasMore()).isTrue();
        }
    }

    // -------------------------------------------------------
    // suspend()
    // -------------------------------------------------------
    @Nested
    @DisplayName("suspend()")
    class Suspend {

        @Test
        @DisplayName("should suspend an active member")
        void shouldSuspendActiveMember() {
            sampleMember.setStatus(MemberStatus.ACTIVE);

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(sampleMember));
            when(memberRepository.save(sampleMember)).thenReturn(sampleMember);

            Member result = memberService.suspend(memberId);

            assertThat(result.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
            verify(memberRepository).save(sampleMember);

            ArgumentCaptor<MemberStatusChangeEvent> eventCaptor = ArgumentCaptor.forClass(MemberStatusChangeEvent.class);
            verify(outboxWriter).write(eventCaptor.capture(), eq("Member"), any(UUID.class));
            MemberStatusChangeEvent event = eventCaptor.getValue();
            assertThat(event.memberId()).isEqualTo(memberId);
            assertThat(event.previousStatus()).isEqualTo("ACTIVE");
            assertThat(event.newStatus()).isEqualTo("SUSPENDED");
        }

        @Test
        @DisplayName("should throw BusinessException when member is already suspended")
        void shouldThrowWhenAlreadySuspended() {
            sampleMember.setStatus(MemberStatus.SUSPENDED);

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(sampleMember));

            assertThatThrownBy(() -> memberService.suspend(memberId))
                    .isInstanceOf(InvalidStateTransitionException.class);

            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when suspending a deactivated member")
        void shouldThrowWhenSuspendingDeactivatedMember() {
            sampleMember.setStatus(MemberStatus.DEACTIVATED);

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(sampleMember));

            assertThatThrownBy(() -> memberService.suspend(memberId))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when member not found")
        void shouldThrowWhenMemberNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(memberRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> memberService.suspend(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------
    // reactivate()
    // -------------------------------------------------------
    @Nested
    @DisplayName("reactivate()")
    class Reactivate {

        @Test
        @DisplayName("should reactivate a suspended member")
        void shouldReactivateSuspendedMember() {
            sampleMember.setStatus(MemberStatus.SUSPENDED);

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(sampleMember));
            when(memberRepository.save(sampleMember)).thenReturn(sampleMember);

            Member result = memberService.reactivate(memberId);

            assertThat(result.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            verify(memberRepository).save(sampleMember);

            ArgumentCaptor<MemberStatusChangeEvent> eventCaptor = ArgumentCaptor.forClass(MemberStatusChangeEvent.class);
            verify(outboxWriter).write(eventCaptor.capture(), eq("Member"), any(UUID.class));
            MemberStatusChangeEvent event = eventCaptor.getValue();
            assertThat(event.memberId()).isEqualTo(memberId);
            assertThat(event.previousStatus()).isEqualTo("SUSPENDED");
            assertThat(event.newStatus()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("should throw BusinessException when member is already active")
        void shouldThrowWhenAlreadyActive() {
            sampleMember.setStatus(MemberStatus.ACTIVE);

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(sampleMember));

            assertThatThrownBy(() -> memberService.reactivate(memberId))
                    .isInstanceOf(InvalidStateTransitionException.class);

            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw InvalidStateTransitionException when member is deactivated")
        void shouldThrowWhenDeactivated() {
            sampleMember.setStatus(MemberStatus.DEACTIVATED);

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(sampleMember));

            assertThatThrownBy(() -> memberService.reactivate(memberId))
                    .isInstanceOf(InvalidStateTransitionException.class);

            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when member not found")
        void shouldThrowWhenMemberNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(memberRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> memberService.reactivate(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
