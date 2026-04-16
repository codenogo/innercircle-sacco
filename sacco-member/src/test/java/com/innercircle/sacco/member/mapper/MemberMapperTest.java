package com.innercircle.sacco.member.mapper;

import com.innercircle.sacco.member.dto.CreateMemberRequest;
import com.innercircle.sacco.member.dto.MemberResponse;
import com.innercircle.sacco.member.dto.UpdateMemberRequest;
import com.innercircle.sacco.member.entity.Member;
import com.innercircle.sacco.member.entity.MemberStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MemberMapperTest {

    private MemberMapper memberMapper;

    @BeforeEach
    void setUp() {
        memberMapper = new MemberMapper();
    }

    // -------------------------------------------------------
    // toEntity(CreateMemberRequest)
    // -------------------------------------------------------
    @Nested
    @DisplayName("toEntity(CreateMemberRequest)")
    class ToEntityFromCreateRequest {

        @Test
        @DisplayName("should map all fields from CreateMemberRequest to Member entity")
        void shouldMapAllFields() {
            CreateMemberRequest request = CreateMemberRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .phone("+254712345678")
                    .nationalId("ID12345")
                    .dateOfBirth(LocalDate.of(1990, 1, 15))
                    .joinDate(LocalDate.of(2024, 1, 1))
                    .build();

            Member result = memberMapper.toEntity(request);

            assertThat(result).isNotNull();
            assertThat(result.getMemberNumber()).isNull();
            assertThat(result.getFirstName()).isEqualTo("John");
            assertThat(result.getLastName()).isEqualTo("Doe");
            assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
            assertThat(result.getPhone()).isEqualTo("+254712345678");
            assertThat(result.getNationalId()).isEqualTo("ID12345");
            assertThat(result.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 15));
            assertThat(result.getJoinDate()).isEqualTo(LocalDate.of(2024, 1, 1));
            // Default values from the Member constructor
            assertThat(result.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(result.getShareBalance()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should set default status to ACTIVE")
        void shouldSetDefaultStatusToActive() {
            CreateMemberRequest request = CreateMemberRequest.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane@example.com")
                    .phone("+254700000000")
                    .nationalId("ID99999")
                    .dateOfBirth(LocalDate.of(1985, 6, 1))
                    .joinDate(LocalDate.of(2024, 6, 1))
                    .build();

            Member result = memberMapper.toEntity(request);

            assertThat(result.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        }

        @Test
        @DisplayName("should set default share balance to zero")
        void shouldSetDefaultShareBalanceToZero() {
            CreateMemberRequest request = CreateMemberRequest.builder()
                    .firstName("Bob")
                    .lastName("Jones")
                    .email("bob@example.com")
                    .phone("+254711111111")
                    .nationalId("ID77777")
                    .dateOfBirth(LocalDate.of(1988, 12, 25))
                    .joinDate(LocalDate.of(2024, 3, 15))
                    .build();

            Member result = memberMapper.toEntity(request);

            assertThat(result.getShareBalance()).isEqualTo(BigDecimal.ZERO);
        }
    }

    // -------------------------------------------------------
    // toEntity(UpdateMemberRequest)
    // -------------------------------------------------------
    @Nested
    @DisplayName("toEntity(UpdateMemberRequest)")
    class ToEntityFromUpdateRequest {

        @Test
        @DisplayName("should map all provided fields from UpdateMemberRequest to Member")
        void shouldMapAllProvidedFields() {
            UpdateMemberRequest request = UpdateMemberRequest.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@example.com")
                    .phone("+254700000000")
                    .nationalId("ID99999")
                    .dateOfBirth(LocalDate.of(1985, 5, 20))
                    .build();

            Member result = memberMapper.toEntity(request);

            assertThat(result.getFirstName()).isEqualTo("Jane");
            assertThat(result.getLastName()).isEqualTo("Smith");
            assertThat(result.getEmail()).isEqualTo("jane.smith@example.com");
            assertThat(result.getPhone()).isEqualTo("+254700000000");
            assertThat(result.getNationalId()).isEqualTo("ID99999");
            assertThat(result.getDateOfBirth()).isEqualTo(LocalDate.of(1985, 5, 20));
        }

        @Test
        @DisplayName("should leave null fields as null when not provided")
        void shouldLeaveNullFieldsAsNull() {
            UpdateMemberRequest request = UpdateMemberRequest.builder().build();

            Member result = memberMapper.toEntity(request);

            assertThat(result.getFirstName()).isNull();
            assertThat(result.getLastName()).isNull();
            assertThat(result.getEmail()).isNull();
            assertThat(result.getPhone()).isNull();
            assertThat(result.getNationalId()).isNull();
            assertThat(result.getDateOfBirth()).isNull();
        }

        @Test
        @DisplayName("should map only firstName when only firstName is provided")
        void shouldMapOnlyFirstName() {
            UpdateMemberRequest request = UpdateMemberRequest.builder()
                    .firstName("Jane")
                    .build();

            Member result = memberMapper.toEntity(request);

            assertThat(result.getFirstName()).isEqualTo("Jane");
            assertThat(result.getLastName()).isNull();
            assertThat(result.getEmail()).isNull();
            assertThat(result.getPhone()).isNull();
            assertThat(result.getNationalId()).isNull();
            assertThat(result.getDateOfBirth()).isNull();
        }

        @Test
        @DisplayName("should map only lastName when only lastName is provided")
        void shouldMapOnlyLastName() {
            UpdateMemberRequest request = UpdateMemberRequest.builder()
                    .lastName("Smith")
                    .build();

            Member result = memberMapper.toEntity(request);

            assertThat(result.getLastName()).isEqualTo("Smith");
            assertThat(result.getFirstName()).isNull();
        }

        @Test
        @DisplayName("should map only email when only email is provided")
        void shouldMapOnlyEmail() {
            UpdateMemberRequest request = UpdateMemberRequest.builder()
                    .email("new@example.com")
                    .build();

            Member result = memberMapper.toEntity(request);

            assertThat(result.getEmail()).isEqualTo("new@example.com");
            assertThat(result.getFirstName()).isNull();
            assertThat(result.getLastName()).isNull();
        }

        @Test
        @DisplayName("should map only phone when only phone is provided")
        void shouldMapOnlyPhone() {
            UpdateMemberRequest request = UpdateMemberRequest.builder()
                    .phone("+254700000000")
                    .build();

            Member result = memberMapper.toEntity(request);

            assertThat(result.getPhone()).isEqualTo("+254700000000");
            assertThat(result.getFirstName()).isNull();
        }

        @Test
        @DisplayName("should map only nationalId when only nationalId is provided")
        void shouldMapOnlyNationalId() {
            UpdateMemberRequest request = UpdateMemberRequest.builder()
                    .nationalId("NEWID")
                    .build();

            Member result = memberMapper.toEntity(request);

            assertThat(result.getNationalId()).isEqualTo("NEWID");
            assertThat(result.getFirstName()).isNull();
        }

        @Test
        @DisplayName("should map only dateOfBirth when only dateOfBirth is provided")
        void shouldMapOnlyDateOfBirth() {
            UpdateMemberRequest request = UpdateMemberRequest.builder()
                    .dateOfBirth(LocalDate.of(1995, 8, 15))
                    .build();

            Member result = memberMapper.toEntity(request);

            assertThat(result.getDateOfBirth()).isEqualTo(LocalDate.of(1995, 8, 15));
            assertThat(result.getFirstName()).isNull();
        }
    }

    // -------------------------------------------------------
    // toResponse(Member)
    // -------------------------------------------------------
    @Nested
    @DisplayName("toResponse(Member)")
    class ToResponse {

        @Test
        @DisplayName("should map all fields from Member entity to MemberResponse")
        void shouldMapAllFields() {
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();

            Member member = new Member(
                    "MBR-001",
                    "John",
                    "Doe",
                    "john.doe@example.com",
                    "+254712345678",
                    "ID12345",
                    LocalDate.of(1990, 1, 15),
                    LocalDate.of(2024, 1, 1)
            );
            member.setId(id);
            member.setShareBalance(new BigDecimal("5000.00"));
            member.setCreatedAt(now);
            member.setUpdatedAt(now);
            member.setCreatedBy("admin");

            MemberResponse result = memberMapper.toResponse(member);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getMemberNumber()).isEqualTo("MBR-001");
            assertThat(result.getFirstName()).isEqualTo("John");
            assertThat(result.getLastName()).isEqualTo("Doe");
            assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
            assertThat(result.getPhone()).isEqualTo("+254712345678");
            assertThat(result.getNationalId()).isEqualTo("ID12345");
            assertThat(result.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 15));
            assertThat(result.getJoinDate()).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(result.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(result.getShareBalance()).isEqualTo(new BigDecimal("5000.00"));
            assertThat(result.getCreatedAt()).isEqualTo(now);
            assertThat(result.getUpdatedAt()).isEqualTo(now);
            assertThat(result.getCreatedBy()).isEqualTo("admin");
        }

        @Test
        @DisplayName("should handle null optional fields gracefully")
        void shouldHandleNullOptionalFields() {
            Member member = new Member();
            member.setId(UUID.randomUUID());

            MemberResponse result = memberMapper.toResponse(member);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getMemberNumber()).isNull();
            assertThat(result.getFirstName()).isNull();
            assertThat(result.getCreatedAt()).isNull();
            assertThat(result.getCreatedBy()).isNull();
        }

        @Test
        @DisplayName("should map SUSPENDED status correctly")
        void shouldMapSuspendedStatus() {
            Member member = new Member(
                    "MBR-005",
                    "Test",
                    "User",
                    "test@example.com",
                    "+254700000000",
                    "ID55555",
                    LocalDate.of(1990, 1, 1),
                    LocalDate.of(2024, 1, 1)
            );
            member.setId(UUID.randomUUID());
            member.setStatus(MemberStatus.SUSPENDED);

            MemberResponse result = memberMapper.toResponse(member);

            assertThat(result.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
        }

        @Test
        @DisplayName("should map DEACTIVATED status correctly")
        void shouldMapDeactivatedStatus() {
            Member member = new Member(
                    "MBR-006",
                    "Test",
                    "User",
                    "test2@example.com",
                    "+254700000001",
                    "ID66666",
                    LocalDate.of(1990, 1, 1),
                    LocalDate.of(2024, 1, 1)
            );
            member.setId(UUID.randomUUID());
            member.setStatus(MemberStatus.DEACTIVATED);

            MemberResponse result = memberMapper.toResponse(member);

            assertThat(result.getStatus()).isEqualTo(MemberStatus.DEACTIVATED);
        }
    }
}
