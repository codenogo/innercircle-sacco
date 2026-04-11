package com.innercircle.sacco.member.service;

import com.innercircle.sacco.common.event.PenaltyAppliedEvent;
import com.innercircle.sacco.common.event.PenaltyPaidEvent;
import com.innercircle.sacco.common.event.PenaltyWaivedEvent;
import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.common.outbox.EventOutboxWriter;
import com.innercircle.sacco.config.service.PolicyConfigResolver;
import com.innercircle.sacco.member.dto.CreateMeetingRequest;
import com.innercircle.sacco.member.dto.MeetingAttendanceEntryRequest;
import com.innercircle.sacco.member.entity.MeetingAttendance;
import com.innercircle.sacco.member.entity.MeetingAttendanceStatus;
import com.innercircle.sacco.member.entity.MeetingFine;
import com.innercircle.sacco.member.entity.MeetingFineType;
import com.innercircle.sacco.member.entity.MeetingSession;
import com.innercircle.sacco.member.entity.MeetingStatus;
import com.innercircle.sacco.member.repository.MeetingAttendanceRepository;
import com.innercircle.sacco.member.repository.MeetingFineRepository;
import com.innercircle.sacco.member.repository.MeetingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingServiceImpl implements MeetingService {

    private final MeetingSessionRepository meetingSessionRepository;
    private final MeetingAttendanceRepository meetingAttendanceRepository;
    private final MeetingFineRepository meetingFineRepository;
    private final PolicyConfigResolver policyConfigResolver;
    private final EventOutboxWriter outboxWriter;

    @Override
    @Transactional(readOnly = true)
    public List<MeetingSession> getAllMeetings() {
        return meetingSessionRepository.findAllByOrderByMeetingDateDesc();
    }

    @Override
    @Transactional
    public MeetingSession createMeeting(CreateMeetingRequest request, String actor) {
        MeetingSession session = new MeetingSession();
        session.setTitle(request.getTitle());
        session.setMeetingDate(request.getMeetingDate());
        session.setStatus(MeetingStatus.OPEN);
        session.setLateThresholdMinutes(resolveInteger(request.getLateThresholdMinutes(), "meeting.fines.late_threshold_minutes"));
        session.setAbsenceFineAmount(resolveDecimal(request.getAbsenceFineAmount(), "meeting.fines.absence_amount"));
        session.setLateFineAmount(resolveDecimal(request.getLateFineAmount(), "meeting.fines.late_amount"));
        session.setUnpaidDailyPenaltyAmount(resolveDecimal(request.getUnpaidDailyPenaltyAmount(), "meeting.fines.unpaid_daily_penalty"));
        session.setNotes(request.getNotes());
        session.setCreatedBy(actor);
        return meetingSessionRepository.save(session);
    }

    @Override
    @Transactional
    public void recordAttendance(UUID meetingId, List<MeetingAttendanceEntryRequest> entries, String actor) {
        MeetingSession session = getMeeting(meetingId);
        if (session.getStatus() == MeetingStatus.FINALIZED) {
            throw new BusinessException("Cannot record attendance for finalized meeting");
        }

        for (MeetingAttendanceEntryRequest entry : entries) {
            MeetingAttendance attendance = meetingAttendanceRepository
                    .findByMeetingIdAndMemberId(meetingId, entry.getMemberId())
                    .orElseGet(MeetingAttendance::new);
            attendance.setMeetingId(meetingId);
            attendance.setMemberId(entry.getMemberId());
            attendance.setAttendanceStatus(entry.getAttendanceStatus());
            attendance.setArrivedAt(entry.getArrivedAt());
            attendance.setRemarks(entry.getRemarks());
            attendance.setCreatedBy(actor);
            meetingAttendanceRepository.save(attendance);
        }
    }

    @Override
    @Transactional
    public MeetingSession finalizeMeeting(UUID meetingId, String actor) {
        MeetingSession session = getMeeting(meetingId);
        if (session.getStatus() == MeetingStatus.FINALIZED) {
            return session;
        }

        List<MeetingAttendance> attendanceRows = meetingAttendanceRepository.findByMeetingId(meetingId);
        List<MeetingFine> generatedFines = new ArrayList<>();

        for (MeetingAttendance attendance : attendanceRows) {
            if (attendance.getAttendanceStatus() == MeetingAttendanceStatus.ABSENT) {
                generatedFines.add(createFine(
                        session,
                        attendance.getMemberId(),
                        attendance.getId(),
                        MeetingFineType.ABSENCE,
                        session.getAbsenceFineAmount(),
                        actor
                ));
            } else if (attendance.getAttendanceStatus() == MeetingAttendanceStatus.LATE) {
                generatedFines.add(createFine(
                        session,
                        attendance.getMemberId(),
                        attendance.getId(),
                        MeetingFineType.LATE,
                        session.getLateFineAmount(),
                        actor
                ));
            }
        }

        if (!generatedFines.isEmpty()) {
            List<MeetingFine> saved = meetingFineRepository.saveAll(generatedFines);
            for (MeetingFine fine : saved) {
                outboxWriter.write(new PenaltyAppliedEvent(
                        fine.getId(),
                        fine.getMemberId(),
                        fine.getAmount(),
                        "MEETING_FINE",
                        UUID.randomUUID(),
                        actor
                ), "MeetingFine", fine.getId());
            }
        }

        session.setStatus(MeetingStatus.FINALIZED);
        session.setFinalizedAt(Instant.now());
        return meetingSessionRepository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingFine> getFines(UUID memberId) {
        if (memberId == null) {
            return meetingFineRepository.findAll();
        }
        return meetingFineRepository.findByMemberId(memberId);
    }

    @Override
    @Transactional
    public MeetingFine settleFine(UUID fineId, String actor) {
        MeetingFine fine = getFine(fineId);
        if (fine.isWaived()) {
            throw new BusinessException("Cannot settle a waived fine");
        }
        if (fine.isSettled()) {
            return fine;
        }
        fine.setSettled(true);
        fine.setSettledAt(Instant.now());
        MeetingFine saved = meetingFineRepository.save(fine);

        outboxWriter.write(new PenaltyPaidEvent(
                saved.getId(),
                saved.getMemberId(),
                saved.getAmount(),
                UUID.randomUUID(),
                actor
        ), "MeetingFine", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public MeetingFine waiveFine(UUID fineId, String reason, String actor) {
        MeetingFine fine = getFine(fineId);
        if (fine.isSettled()) {
            throw new BusinessException("Cannot waive a settled fine");
        }
        if (fine.isWaived()) {
            return fine;
        }
        String waiverReason = reason == null || reason.isBlank() ? "Manual waiver" : reason.trim();
        fine.setWaived(true);
        fine.setWaivedBy(actor);
        fine.setWaivedAt(Instant.now());
        fine.setWaivedReason(waiverReason);
        MeetingFine saved = meetingFineRepository.save(fine);

        outboxWriter.write(new PenaltyWaivedEvent(
                saved.getId(),
                saved.getMemberId(),
                saved.getAmount(),
                waiverReason,
                UUID.randomUUID(),
                actor
        ), "MeetingFine", saved.getId());
        return saved;
    }

    private MeetingSession getMeeting(UUID meetingId) {
        return meetingSessionRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("MeetingSession", meetingId));
    }

    private MeetingFine getFine(UUID fineId) {
        return meetingFineRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("MeetingFine", fineId));
    }

    private MeetingFine createFine(
            MeetingSession session,
            UUID memberId,
            UUID attendanceId,
            MeetingFineType type,
            BigDecimal amount,
            String actor
    ) {
        MeetingFine fine = new MeetingFine();
        fine.setMeetingId(session.getId());
        fine.setAttendanceId(attendanceId);
        fine.setMemberId(memberId);
        fine.setFineType(type);
        fine.setAmount(amount);
        fine.setDueDate(session.getMeetingDate().plusDays(7));
        fine.setCreatedBy(actor);
        return fine;
    }

    private Integer resolveInteger(Integer provided, String policyKey) {
        return provided != null ? provided : policyConfigResolver.requireIntAtLeast(policyKey, 0);
    }

    private BigDecimal resolveDecimal(BigDecimal provided, String policyKey) {
        return provided != null ? provided : policyConfigResolver.requireNonNegativeDecimal(policyKey);
    }
}
