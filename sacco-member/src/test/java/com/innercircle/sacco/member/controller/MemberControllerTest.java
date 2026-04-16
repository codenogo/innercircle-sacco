package com.innercircle.sacco.member.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.member.dto.CreateMemberRequest;
import com.innercircle.sacco.member.dto.MemberResponse;
import com.innercircle.sacco.member.dto.UpdateMemberRequest;
import com.innercircle.sacco.member.entity.Member;
import com.innercircle.sacco.member.entity.MemberStatus;
import com.innercircle.sacco.member.mapper.MemberMapper;
import com.innercircle.sacco.member.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    @Mock
    private MemberService memberService;

    @Mock
    private MemberMapper memberMapper;

    @InjectMocks
    private MemberController memberController;

    private Member sampleMember;
    private MemberResponse sampleResponse;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        Instant now = Instant.now();

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

        sampleResponse = MemberResponse.builder()
                .id(memberId)
                .memberNumber("MBR-001")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("+254712345678")
                .nationalId("ID12345")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .joinDate(LocalDate.of(2024, 1, 1))
                .status(MemberStatus.ACTIVE)
                .shareBalance(BigDecimal.ZERO)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    // -------------------------------------------------------
    // create()
    // -------------------------------------------------------
    @Nested
    @DisplayName("POST /api/v1/members")
    class CreateEndpoint {

        @Test
        @DisplayName("should create member and return ApiResponse with created member")
        void shouldCreateMember() {
            CreateMemberRequest request = CreateMemberRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .phone("+254712345678")
                    .nationalId("ID12345")
                    .dateOfBirth(LocalDate.of(1990, 1, 15))
                    .joinDate(LocalDate.of(2024, 1, 1))
                    .build();

            when(memberMapper.toEntity(request)).thenReturn(sampleMember);
            when(memberService.create(sampleMember)).thenReturn(sampleMember);
            when(memberMapper.toResponse(sampleMember)).thenReturn(sampleResponse);

            ApiResponse<MemberResponse> result = memberController.create(request);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(sampleResponse);
            assertThat(result.getMessage()).isEqualTo("Member created successfully");
            verify(memberMapper).toEntity(request);
            verify(memberService).create(sampleMember);
            verify(memberMapper).toResponse(sampleMember);
        }
    }

    // -------------------------------------------------------
    // list()
    // -------------------------------------------------------
    @Nested
    @DisplayName("GET /api/v1/members")
    class ListEndpoint {

        @Test
        @DisplayName("should return paginated list of members")
        void shouldReturnPaginatedList() {
            CursorPage<Member> memberPage = CursorPage.of(
                    List.of(sampleMember),
                    null,
                    false
            );

            when(memberService.list(null, 20)).thenReturn(memberPage);
            when(memberMapper.toResponse(sampleMember)).thenReturn(sampleResponse);

            ApiResponse<CursorPage<MemberResponse>> result = memberController.list(null, 20);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getItems()).hasSize(1);
            assertThat(result.getData().getItems().get(0)).isEqualTo(sampleResponse);
            assertThat(result.getData().isHasMore()).isFalse();
        }

        @Test
        @DisplayName("should pass cursor and size to service")
        void shouldPassCursorAndSize() {
            String cursor = UUID.randomUUID().toString();
            CursorPage<Member> emptyPage = CursorPage.of(List.of(), null, false);

            when(memberService.list(cursor, 10)).thenReturn(emptyPage);

            ApiResponse<CursorPage<MemberResponse>> result = memberController.list(cursor, 10);

            assertThat(result.getData().getItems()).isEmpty();
            verify(memberService).list(cursor, 10);
        }

        @Test
        @DisplayName("should map all members in the page to responses")
        void shouldMapAllMembers() {
            Member member2 = new Member(
                    "MBR-002", "Jane", "Smith", "jane@example.com",
                    "+254700000000", "ID99999",
                    LocalDate.of(1985, 5, 20), LocalDate.of(2024, 2, 1)
            );
            member2.setId(UUID.randomUUID());

            MemberResponse response2 = MemberResponse.builder()
                    .id(member2.getId())
                    .memberNumber("MBR-002")
                    .firstName("Jane")
                    .build();

            String nextCursor = member2.getId().toString();
            CursorPage<Member> memberPage = CursorPage.of(
                    List.of(sampleMember, member2),
                    nextCursor,
                    true
            );

            when(memberService.list(null, 2)).thenReturn(memberPage);
            when(memberMapper.toResponse(sampleMember)).thenReturn(sampleResponse);
            when(memberMapper.toResponse(member2)).thenReturn(response2);

            ApiResponse<CursorPage<MemberResponse>> result = memberController.list(null, 2);

            assertThat(result.getData().getItems()).hasSize(2);
            assertThat(result.getData().isHasMore()).isTrue();
            assertThat(result.getData().getNextCursor()).isEqualTo(nextCursor);
        }
    }

    // -------------------------------------------------------
    // getById()
    // -------------------------------------------------------
    @Nested
    @DisplayName("GET /api/v1/members/{id}")
    class GetByIdEndpoint {

        @Test
        @DisplayName("should return member by ID")
        void shouldReturnMemberById() {
            when(memberService.findById(memberId)).thenReturn(sampleMember);
            when(memberMapper.toResponse(sampleMember)).thenReturn(sampleResponse);

            ApiResponse<MemberResponse> result = memberController.getById(memberId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(sampleResponse);
            assertThat(result.getData().getId()).isEqualTo(memberId);
            verify(memberService).findById(memberId);
        }
    }

    // -------------------------------------------------------
    // update()
    // -------------------------------------------------------
    @Nested
    @DisplayName("PUT /api/v1/members/{id}")
    class UpdateEndpoint {

        @Test
        @DisplayName("should update member and return updated response")
        void shouldUpdateMember() {
            UpdateMemberRequest request = UpdateMemberRequest.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .build();

            Member updateData = new Member();
            updateData.setFirstName("Jane");
            updateData.setLastName("Smith");

            Member updatedMember = new Member(
                    "MBR-001", "Jane", "Smith", "john.doe@example.com",
                    "+254712345678", "ID12345",
                    LocalDate.of(1990, 1, 15), LocalDate.of(2024, 1, 1)
            );
            updatedMember.setId(memberId);

            MemberResponse updatedResponse = MemberResponse.builder()
                    .id(memberId)
                    .memberNumber("MBR-001")
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("john.doe@example.com")
                    .status(MemberStatus.ACTIVE)
                    .build();

            when(memberMapper.toEntity(request)).thenReturn(updateData);
            when(memberService.update(memberId, updateData)).thenReturn(updatedMember);
            when(memberMapper.toResponse(updatedMember)).thenReturn(updatedResponse);

            ApiResponse<MemberResponse> result = memberController.update(memberId, request);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getFirstName()).isEqualTo("Jane");
            assertThat(result.getData().getLastName()).isEqualTo("Smith");
            assertThat(result.getMessage()).isEqualTo("Member updated successfully");
            verify(memberMapper).toEntity(request);
            verify(memberService).update(memberId, updateData);
        }
    }

    // -------------------------------------------------------
    // suspend()
    // -------------------------------------------------------
    @Nested
    @DisplayName("PATCH /api/v1/members/{id}/suspend")
    class SuspendEndpoint {

        @Test
        @DisplayName("should suspend member and return suspended response")
        void shouldSuspendMember() {
            Member suspendedMember = new Member(
                    "MBR-001", "John", "Doe", "john.doe@example.com",
                    "+254712345678", "ID12345",
                    LocalDate.of(1990, 1, 15), LocalDate.of(2024, 1, 1)
            );
            suspendedMember.setId(memberId);
            suspendedMember.setStatus(MemberStatus.SUSPENDED);

            MemberResponse suspendedResponse = MemberResponse.builder()
                    .id(memberId)
                    .memberNumber("MBR-001")
                    .status(MemberStatus.SUSPENDED)
                    .build();

            when(memberService.suspend(memberId)).thenReturn(suspendedMember);
            when(memberMapper.toResponse(suspendedMember)).thenReturn(suspendedResponse);

            ApiResponse<MemberResponse> result = memberController.suspend(memberId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getStatus()).isEqualTo(MemberStatus.SUSPENDED);
            assertThat(result.getMessage()).isEqualTo("Member suspended successfully");
            verify(memberService).suspend(memberId);
        }
    }

    // -------------------------------------------------------
    // reactivate()
    // -------------------------------------------------------
    @Nested
    @DisplayName("PATCH /api/v1/members/{id}/reactivate")
    class ReactivateEndpoint {

        @Test
        @DisplayName("should reactivate member and return reactivated response")
        void shouldReactivateMember() {
            Member reactivatedMember = new Member(
                    "MBR-001", "John", "Doe", "john.doe@example.com",
                    "+254712345678", "ID12345",
                    LocalDate.of(1990, 1, 15), LocalDate.of(2024, 1, 1)
            );
            reactivatedMember.setId(memberId);
            reactivatedMember.setStatus(MemberStatus.ACTIVE);

            MemberResponse reactivatedResponse = MemberResponse.builder()
                    .id(memberId)
                    .memberNumber("MBR-001")
                    .status(MemberStatus.ACTIVE)
                    .build();

            when(memberService.reactivate(memberId)).thenReturn(reactivatedMember);
            when(memberMapper.toResponse(reactivatedMember)).thenReturn(reactivatedResponse);

            ApiResponse<MemberResponse> result = memberController.reactivate(memberId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getStatus()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(result.getMessage()).isEqualTo("Member reactivated successfully");
            verify(memberService).reactivate(memberId);
        }
    }
}
