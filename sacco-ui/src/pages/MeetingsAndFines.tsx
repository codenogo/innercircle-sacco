import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { DataTable, type ColumnDef } from '../components/DataTable'
import { ApiError } from '../services/apiClient'
import {
  createMeeting,
  finalizeMeeting,
  getMeetingFines,
  recordMeetingAttendance,
  settleMeetingFine,
  waiveMeetingFine,
} from '../services/policyWorkflowService'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import type { MeetingAttendanceStatus, MeetingFineResponse, MeetingResponse } from '../types/policyWorkflows'
import './Operations.css'

interface Feedback {
  type: 'success' | 'error'
  text: string
}

interface MeetingFormState {
  title: string
  meetingDate: string
  lateThresholdMinutes: string
  absenceFineAmount: string
  lateFineAmount: string
  unpaidDailyPenaltyAmount: string
  notes: string
}

interface AttendanceFormState {
  meetingId: string
  memberId: string
  attendanceStatus: MeetingAttendanceStatus
  arrivedAt: string
  remarks: string
}

const EMPTY_MEETING_FORM: MeetingFormState = {
  title: '',
  meetingDate: '',
  lateThresholdMinutes: '',
  absenceFineAmount: '',
  lateFineAmount: '',
  unpaidDailyPenaltyAmount: '',
  notes: '',
}

const EMPTY_ATTENDANCE_FORM: AttendanceFormState = {
  meetingId: '',
  memberId: '',
  attendanceStatus: 'PRESENT',
  arrivedAt: '',
  remarks: '',
}

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return fallback
}

function formatMoney(value: number) {
  return value.toLocaleString('en-KE')
}

function formatDateTime(value: string | null) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleString('en-KE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function MeetingsAndFines() {
  const { request } = useAuthenticatedApi()

  const [meetingForm, setMeetingForm] = useState<MeetingFormState>(EMPTY_MEETING_FORM)
  const [attendanceForm, setAttendanceForm] = useState<AttendanceFormState>(EMPTY_ATTENDANCE_FORM)
  const [finalizeMeetingId, setFinalizeMeetingId] = useState('')
  const [finesMemberId, setFinesMemberId] = useState('')
  const [fines, setFines] = useState<MeetingFineResponse[]>([])
  const [latestMeeting, setLatestMeeting] = useState<MeetingResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [feedback, setFeedback] = useState<Feedback | null>(null)

  const fineColumns: ColumnDef<MeetingFineResponse>[] = [
    { key: 'member', header: 'Member', render: row => row.memberId },
    { key: 'type', header: 'Type', render: row => row.fineType },
    { key: 'amount', header: 'Amount', className: 'ledger-table-amount', render: row => `KES ${formatMoney(row.amount)}` },
    { key: 'dueDate', header: 'Due Date', render: row => row.dueDate ?? '-' },
    { key: 'status', header: 'Status', render: row => row.waived ? 'Waived' : row.settled ? 'Settled' : 'Open' },
    {
      key: 'actions',
      header: 'Actions',
      render: row => (
        <div className="settings-row-actions">
          <button
            type="button"
            className="btn btn--secondary btn--sm"
            disabled={submitting || row.settled || row.waived}
            onClick={() => void handleSettleFine(row.id)}
          >
            Settle
          </button>
          <button
            type="button"
            className="btn btn--ghost btn--sm"
            disabled={submitting || row.waived}
            onClick={() => void handleWaiveFine(row.id)}
          >
            Waive
          </button>
        </div>
      ),
    },
  ]

  const loadFines = useCallback(async (memberId?: string) => {
    setLoading(true)
    try {
      const data = await getMeetingFines(memberId, request)
      setFines(data)
      setFeedback(null)
    } catch (error) {
      setFeedback({ type: 'error', text: toErrorMessage(error, 'Unable to load meeting fines.') })
    } finally {
      setLoading(false)
    }
  }, [request])

  useEffect(() => {
    void loadFines()
  }, [loadFines])

  async function handleCreateMeeting(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setFeedback(null)
    try {
      const created = await createMeeting({
        title: meetingForm.title.trim(),
        meetingDate: meetingForm.meetingDate,
        lateThresholdMinutes: meetingForm.lateThresholdMinutes ? Number.parseInt(meetingForm.lateThresholdMinutes, 10) : undefined,
        absenceFineAmount: meetingForm.absenceFineAmount ? Number(meetingForm.absenceFineAmount) : undefined,
        lateFineAmount: meetingForm.lateFineAmount ? Number(meetingForm.lateFineAmount) : undefined,
        unpaidDailyPenaltyAmount: meetingForm.unpaidDailyPenaltyAmount ? Number(meetingForm.unpaidDailyPenaltyAmount) : undefined,
        notes: meetingForm.notes.trim() || undefined,
      }, request)
      setLatestMeeting(created)
      setFinalizeMeetingId(created.id)
      setAttendanceForm(prev => ({ ...prev, meetingId: created.id }))
      setMeetingForm(EMPTY_MEETING_FORM)
      setFeedback({ type: 'success', text: `Meeting "${created.title}" created.` })
    } catch (error) {
      setFeedback({ type: 'error', text: toErrorMessage(error, 'Unable to create meeting.') })
    } finally {
      setSubmitting(false)
    }
  }

  async function handleRecordAttendance(event: FormEvent) {
    event.preventDefault()
    if (!attendanceForm.meetingId || !attendanceForm.memberId) return
    setSubmitting(true)
    setFeedback(null)
    try {
      await recordMeetingAttendance(attendanceForm.meetingId, {
        entries: [{
          memberId: attendanceForm.memberId,
          attendanceStatus: attendanceForm.attendanceStatus,
          arrivedAt: attendanceForm.arrivedAt ? new Date(attendanceForm.arrivedAt).toISOString() : undefined,
          remarks: attendanceForm.remarks.trim() || undefined,
        }],
      }, request)
      setAttendanceForm(prev => ({ ...EMPTY_ATTENDANCE_FORM, meetingId: prev.meetingId }))
      setFeedback({ type: 'success', text: 'Attendance recorded.' })
    } catch (error) {
      setFeedback({ type: 'error', text: toErrorMessage(error, 'Unable to record attendance.') })
    } finally {
      setSubmitting(false)
    }
  }

  async function handleFinalizeMeeting(event: FormEvent) {
    event.preventDefault()
    if (!finalizeMeetingId.trim()) return
    setSubmitting(true)
    setFeedback(null)
    try {
      const result = await finalizeMeeting(finalizeMeetingId.trim(), request)
      setLatestMeeting(result)
      await loadFines(finesMemberId.trim() || undefined)
      setFeedback({ type: 'success', text: `Meeting ${result.id} finalized and fines generated.` })
    } catch (error) {
      setFeedback({ type: 'error', text: toErrorMessage(error, 'Unable to finalize meeting.') })
    } finally {
      setSubmitting(false)
    }
  }

  async function handleSettleFine(fineId: string) {
    setSubmitting(true)
    setFeedback(null)
    try {
      const updated = await settleMeetingFine(fineId, request)
      setFines(prev => prev.map(item => (item.id === updated.id ? updated : item)))
      setFeedback({ type: 'success', text: 'Meeting fine settled.' })
    } catch (error) {
      setFeedback({ type: 'error', text: toErrorMessage(error, 'Unable to settle fine.') })
    } finally {
      setSubmitting(false)
    }
  }

  async function handleWaiveFine(fineId: string) {
    const reason = window.prompt('Waiver reason (optional):') ?? ''
    setSubmitting(true)
    setFeedback(null)
    try {
      const updated = await waiveMeetingFine(fineId, reason.trim() || undefined, request)
      setFines(prev => prev.map(item => (item.id === updated.id ? updated : item)))
      setFeedback({ type: 'success', text: 'Meeting fine waived.' })
    } catch (error) {
      setFeedback({ type: 'error', text: toErrorMessage(error, 'Unable to waive fine.') })
    } finally {
      setSubmitting(false)
    }
  }

  async function handleFilterFines(event: FormEvent) {
    event.preventDefault()
    await loadFines(finesMemberId.trim() || undefined)
  }

  return (
    <div className="ops-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Meetings & Fines</h1>
          <p className="page-subtitle">Capture meetings, attendance, and fine settlement workflows.</p>
        </div>
      </div>

      <hr className="rule rule--strong" />

      {feedback && (
        <div className={`ops-feedback ops-feedback--${feedback.type}`} role="status">{feedback.text}</div>
      )}

      <section className="page-section">
        <span className="page-section-title">Create Meeting</span>
        <hr className="rule" />
        <form className="modal-form" onSubmit={event => void handleCreateMeeting(event)}>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Title</label>
              <input className="field-input" value={meetingForm.title} onChange={event => setMeetingForm(prev => ({ ...prev, title: event.target.value }))} required />
            </div>
            <div className="field">
              <label className="field-label field-label--required">Meeting Date</label>
              <input className="field-input" type="date" value={meetingForm.meetingDate} onChange={event => setMeetingForm(prev => ({ ...prev, meetingDate: event.target.value }))} required />
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label className="field-label">Absence Fine (KES)</label>
              <input className="field-input" type="number" min={0} step="0.01" value={meetingForm.absenceFineAmount} onChange={event => setMeetingForm(prev => ({ ...prev, absenceFineAmount: event.target.value }))} />
            </div>
            <div className="field">
              <label className="field-label">Late Fine (KES)</label>
              <input className="field-input" type="number" min={0} step="0.01" value={meetingForm.lateFineAmount} onChange={event => setMeetingForm(prev => ({ ...prev, lateFineAmount: event.target.value }))} />
            </div>
            <div className="field">
              <label className="field-label">Unpaid Daily Penalty (KES)</label>
              <input className="field-input" type="number" min={0} step="0.01" value={meetingForm.unpaidDailyPenaltyAmount} onChange={event => setMeetingForm(prev => ({ ...prev, unpaidDailyPenaltyAmount: event.target.value }))} />
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label className="field-label">Late Threshold (Minutes)</label>
              <input className="field-input" type="number" min={0} step="1" value={meetingForm.lateThresholdMinutes} onChange={event => setMeetingForm(prev => ({ ...prev, lateThresholdMinutes: event.target.value }))} />
            </div>
            <div className="field">
              <label className="field-label">Notes</label>
              <input className="field-input" value={meetingForm.notes} onChange={event => setMeetingForm(prev => ({ ...prev, notes: event.target.value }))} />
            </div>
          </div>
          <button type="submit" className="btn btn--primary" disabled={submitting}>Create Meeting</button>
        </form>
      </section>

      <section className="page-section">
        <span className="page-section-title">Record Attendance</span>
        <hr className="rule" />
        <form className="modal-form" onSubmit={event => void handleRecordAttendance(event)}>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Meeting ID</label>
              <input className="field-input" value={attendanceForm.meetingId} onChange={event => setAttendanceForm(prev => ({ ...prev, meetingId: event.target.value }))} required />
            </div>
            <div className="field">
              <label className="field-label field-label--required">Member ID</label>
              <input className="field-input" value={attendanceForm.memberId} onChange={event => setAttendanceForm(prev => ({ ...prev, memberId: event.target.value }))} required />
            </div>
            <div className="field">
              <label className="field-label field-label--required">Status</label>
              <select className="field-input" value={attendanceForm.attendanceStatus} onChange={event => setAttendanceForm(prev => ({ ...prev, attendanceStatus: event.target.value as MeetingAttendanceStatus }))}>
                <option value="PRESENT">Present</option>
                <option value="LATE">Late</option>
                <option value="ABSENT">Absent</option>
                <option value="EXCUSED">Excused</option>
              </select>
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label className="field-label">Arrived At</label>
              <input className="field-input" type="datetime-local" value={attendanceForm.arrivedAt} onChange={event => setAttendanceForm(prev => ({ ...prev, arrivedAt: event.target.value }))} />
            </div>
            <div className="field">
              <label className="field-label">Remarks</label>
              <input className="field-input" value={attendanceForm.remarks} onChange={event => setAttendanceForm(prev => ({ ...prev, remarks: event.target.value }))} />
            </div>
          </div>
          <button type="submit" className="btn btn--secondary" disabled={submitting}>Record Attendance</button>
        </form>
      </section>

      <section className="page-section">
        <span className="page-section-title">Finalize Meeting</span>
        <hr className="rule" />
        <form className="modal-form" onSubmit={event => void handleFinalizeMeeting(event)}>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Meeting ID</label>
              <input className="field-input" value={finalizeMeetingId} onChange={event => setFinalizeMeetingId(event.target.value)} required />
            </div>
            <div className="field" style={{ alignSelf: 'end' }}>
              <button type="submit" className="btn btn--primary" disabled={submitting}>Finalize</button>
            </div>
          </div>
        </form>
        {latestMeeting && (
          <div className="settings-row settings-row--compact">
            <span className="settings-row-label">Latest Meeting</span>
            <span className="settings-row-value">
              {latestMeeting.title} ({latestMeeting.status}) - {formatDateTime(latestMeeting.updatedAt)}
            </span>
          </div>
        )}
      </section>

      <section className="page-section">
        <span className="page-section-title">Meeting Fines</span>
        <hr className="rule" />
        <form className="modal-form" onSubmit={event => void handleFilterFines(event)}>
          <div className="field-row">
            <div className="field">
              <label className="field-label">Filter by Member ID</label>
              <input className="field-input" value={finesMemberId} onChange={event => setFinesMemberId(event.target.value)} placeholder="Optional member id" />
            </div>
            <div className="field" style={{ alignSelf: 'end' }}>
              <button type="submit" className="btn btn--secondary" disabled={submitting}>Refresh</button>
            </div>
          </div>
        </form>
        {loading ? (
          <div className="ops-feedback">Loading fines...</div>
        ) : (
          <DataTable
            columns={fineColumns}
            data={fines}
            getRowKey={row => row.id}
            emptyMessage="No meeting fines found."
          />
        )}
      </section>
    </div>
  )
}
