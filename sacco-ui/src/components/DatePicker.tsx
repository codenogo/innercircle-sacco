import { useState, useEffect, useRef, type CSSProperties } from 'react'
import { createPortal } from 'react-dom'
import { Calendar, ChevronLeft, ChevronRight } from 'lucide-react'
import './DatePicker.css'

interface DatePickerProps {
  value: string            // ISO date string YYYY-MM-DD
  onChange: (val: string) => void
  required?: boolean
}

const MONTHS = ['January','February','March','April','May','June','July','August','September','October','November','December']
const WEEKDAYS = ['Mo','Tu','We','Th','Fr','Sa','Su']
const PANEL_GAP = 4
const VIEWPORT_MARGIN = 8
const PANEL_HEIGHT_ESTIMATE = 320

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max)
}

function toISO(y: number, m: number, d: number) {
  return `${y}-${String(m + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`
}

function formatDisplay(iso: string) {
  if (!iso) return ''
  const [y, m, d] = iso.split('-').map(Number)
  return `${d} ${MONTHS[m - 1]?.slice(0, 3)} ${y}`
}

function getDaysInMonth(year: number, month: number) {
  return new Date(year, month + 1, 0).getDate()
}

/** Monday = 0, Sunday = 6 */
function getStartDay(year: number, month: number) {
  const d = new Date(year, month, 1).getDay()
  return d === 0 ? 6 : d - 1
}

export function DatePicker({ value, onChange, required }: DatePickerProps) {
  const today = new Date()
  const todayISO = toISO(today.getFullYear(), today.getMonth(), today.getDate())

  // Parse the value into viewable month/year
  const parsed = value ? value.split('-').map(Number) : [today.getFullYear(), today.getMonth() + 1, today.getDate()]
  const [viewYear, setViewYear] = useState(parsed[0])
  const [viewMonth, setViewMonth] = useState(parsed[1] - 1) // 0-indexed
  const [open, setOpen] = useState(false)
  const [panelStyle, setPanelStyle] = useState<CSSProperties>({})
  const [panelPlacement, setPanelPlacement] = useState<'top' | 'bottom'>('bottom')

  const wrapRef = useRef<HTMLDivElement>(null)
  const triggerRef = useRef<HTMLButtonElement>(null)
  const panelRef = useRef<HTMLDivElement>(null)

  // Close on outside click
  useEffect(() => {
    if (!open) return
    function handleClick(e: MouseEvent) {
      const target = e.target as Node
      if (wrapRef.current?.contains(target)) return
      if (panelRef.current?.contains(target)) return
      setOpen(false)
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [open])

  useEffect(() => {
    if (!open) return

    function updatePanelPosition() {
      if (!triggerRef.current) return

      const triggerRect = triggerRef.current.getBoundingClientRect()
      const panelWidth = Math.max(260, Math.min(320, triggerRect.width))
      const spaceBelow = window.innerHeight - triggerRect.bottom - VIEWPORT_MARGIN
      const spaceAbove = triggerRect.top - VIEWPORT_MARGIN
      const openAbove = spaceBelow < PANEL_HEIGHT_ESTIMATE && spaceAbove > spaceBelow

      const desiredTop = openAbove
        ? triggerRect.top - PANEL_HEIGHT_ESTIMATE - PANEL_GAP
        : triggerRect.bottom + PANEL_GAP
      const maxTop = Math.max(VIEWPORT_MARGIN, window.innerHeight - PANEL_HEIGHT_ESTIMATE - VIEWPORT_MARGIN)
      const top = clamp(desiredTop, VIEWPORT_MARGIN, maxTop)
      const maxLeft = Math.max(VIEWPORT_MARGIN, window.innerWidth - panelWidth - VIEWPORT_MARGIN)
      const left = clamp(triggerRect.left, VIEWPORT_MARGIN, maxLeft)

      setPanelPlacement(openAbove ? 'top' : 'bottom')
      setPanelStyle({
        position: 'fixed',
        top,
        left,
        width: panelWidth,
        zIndex: 2100,
      })
    }

    updatePanelPosition()
    window.addEventListener('resize', updatePanelPosition)
    window.addEventListener('scroll', updatePanelPosition, true)
    return () => {
      window.removeEventListener('resize', updatePanelPosition)
      window.removeEventListener('scroll', updatePanelPosition, true)
    }
  }, [open])

  // Close on Escape
  useEffect(() => {
    if (!open) return
    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape') setOpen(false)
    }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  }, [open])

  function prevMonth() {
    if (viewMonth === 0) { setViewMonth(11); setViewYear(y => y - 1) }
    else setViewMonth(m => m - 1)
  }

  function nextMonth() {
    if (viewMonth === 11) { setViewMonth(0); setViewYear(y => y + 1) }
    else setViewMonth(m => m + 1)
  }

  function selectDay(day: number, monthOffset: number) {
    let m = viewMonth + monthOffset
    let y = viewYear
    if (m < 0) { m = 11; y -= 1 }
    if (m > 11) { m = 0; y += 1 }
    const iso = toISO(y, m, day)
    onChange(iso)
    setOpen(false)
  }

  function goToday() {
    onChange(todayISO)
    setViewYear(today.getFullYear())
    setViewMonth(today.getMonth())
    setOpen(false)
  }

  // Build the calendar grid
  const daysInMonth = getDaysInMonth(viewYear, viewMonth)
  const startDay = getStartDay(viewYear, viewMonth)
  const prevMonthDays = getDaysInMonth(viewYear, viewMonth - 1 < 0 ? 11 : viewMonth - 1)

  const cells: { day: number; offset: number }[] = []

  // Leading days from previous month
  for (let i = startDay - 1; i >= 0; i--) {
    cells.push({ day: prevMonthDays - i, offset: -1 })
  }

  // Current month
  for (let d = 1; d <= daysInMonth; d++) {
    cells.push({ day: d, offset: 0 })
  }

  // Trailing days to fill to 6 rows (42 cells) or nearest complete row
  const remaining = 7 - (cells.length % 7)
  if (remaining < 7) {
    for (let d = 1; d <= remaining; d++) {
      cells.push({ day: d, offset: 1 })
    }
  }

  return (
    <div className="datepicker" ref={wrapRef}>
      <button
        ref={triggerRef}
        type="button"
        className={`datepicker-trigger ${open ? 'datepicker-trigger--open' : ''} ${!value ? 'datepicker-trigger--placeholder' : ''}`}
        onClick={() => setOpen(o => !o)}
        aria-haspopup="dialog"
        aria-expanded={open}
      >
        <Calendar size={14} strokeWidth={1.75} />
        <span className="datepicker-trigger-text">
          {value ? formatDisplay(value) : 'Select date'}
        </span>
      </button>

      {/* Hidden native input for form validation */}
      {required && <input type="text" required value={value} tabIndex={-1} style={{ position: 'absolute', opacity: 0, height: 0, width: 0, pointerEvents: 'none' }} onChange={() => {}} />}

      {open && createPortal(
        <div
          ref={panelRef}
          className={`datepicker-panel ${panelPlacement === 'top' ? 'datepicker-panel--top' : ''}`}
          style={panelStyle}
          role="dialog"
          aria-label="Choose date"
        >
          <div className="datepicker-header">
            <button type="button" className="datepicker-nav" onClick={prevMonth} aria-label="Previous month">
              <ChevronLeft size={16} strokeWidth={2} />
            </button>
            <span className="datepicker-month-label">
              {MONTHS[viewMonth]} {viewYear}
            </span>
            <button type="button" className="datepicker-nav" onClick={nextMonth} aria-label="Next month">
              <ChevronRight size={16} strokeWidth={2} />
            </button>
          </div>

          <div className="datepicker-weekdays">
            {WEEKDAYS.map(d => <span key={d} className="datepicker-weekday">{d}</span>)}
          </div>

          <div className="datepicker-days" role="grid">
            {cells.map((cell, i) => {
              const cellISO = toISO(
                viewYear + (viewMonth + cell.offset < 0 ? -1 : viewMonth + cell.offset > 11 ? 1 : 0),
                ((viewMonth + cell.offset) + 12) % 12,
                cell.day
              )
              const isSelected = cellISO === value
              const isToday = cellISO === todayISO
              const isOutside = cell.offset !== 0

              return (
                <button
                  key={i}
                  type="button"
                  className={[
                    'datepicker-day',
                    isOutside && 'datepicker-day--outside',
                    isToday && !isSelected && 'datepicker-day--today',
                    isSelected && 'datepicker-day--selected',
                  ].filter(Boolean).join(' ')}
                  onClick={() => selectDay(cell.day, cell.offset)}
                  aria-label={`${cell.day} ${MONTHS[((viewMonth + cell.offset) + 12) % 12]}`}
                  aria-pressed={isSelected}
                >
                  {cell.day}
                </button>
              )
            })}
          </div>

          <div className="datepicker-footer">
            <button type="button" className="datepicker-today-btn" onClick={goToday}>
              Today
            </button>
          </div>
        </div>,
        document.body
      )}
    </div>
  )
}
