import { useState, useEffect, useRef } from 'react'
import { Calendar, ChevronLeft, ChevronRight } from 'lucide-react'
import './MonthPicker.css'

interface MonthPickerProps {
  /** Value in YYYY-MM format */
  value: string
  onChange: (val: string) => void
}

const MONTHS_SHORT = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']
const MONTHS_FULL = ['January','February','March','April','May','June','July','August','September','October','November','December']

export function MonthPicker({ value, onChange }: MonthPickerProps) {
  const today = new Date()
  const currentMonth = today.getMonth()
  const currentYear = today.getFullYear()

  const [selYear, selMonth] = value.split('-').map(Number) // selMonth is 1-indexed from YYYY-MM
  const [viewYear, setViewYear] = useState(selYear)
  const [open, setOpen] = useState(false)

  const wrapRef = useRef<HTMLDivElement>(null)

  // Close on outside click
  useEffect(() => {
    if (!open) return
    function handleClick(e: MouseEvent) {
      if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
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

  function selectMonth(monthIndex: number) {
    const val = `${viewYear}-${String(monthIndex + 1).padStart(2, '0')}`
    onChange(val)
    setOpen(false)
  }

  const displayText = `${MONTHS_FULL[selMonth - 1]} ${selYear}`

  return (
    <div className="monthpicker" ref={wrapRef}>
      <button
        type="button"
        className={`monthpicker-trigger ${open ? 'monthpicker-trigger--open' : ''}`}
        onClick={() => setOpen(o => !o)}
        aria-haspopup="dialog"
        aria-expanded={open}
      >
        <Calendar size={14} strokeWidth={1.75} />
        {displayText}
      </button>

      {open && (
        <div className="monthpicker-panel" role="dialog" aria-label="Choose month">
          <div className="monthpicker-header">
            <button type="button" className="monthpicker-nav" onClick={() => setViewYear(y => y - 1)} aria-label="Previous year">
              <ChevronLeft size={16} strokeWidth={2} />
            </button>
            <span className="monthpicker-year">{viewYear}</span>
            <button type="button" className="monthpicker-nav" onClick={() => setViewYear(y => y + 1)} aria-label="Next year">
              <ChevronRight size={16} strokeWidth={2} />
            </button>
          </div>

          <div className="monthpicker-grid">
            {MONTHS_SHORT.map((label, i) => {
              const isSelected = viewYear === selYear && i === selMonth - 1
              const isCurrent = viewYear === currentYear && i === currentMonth

              return (
                <button
                  key={label}
                  type="button"
                  className={[
                    'monthpicker-month',
                    isCurrent && !isSelected && 'monthpicker-month--current',
                    isSelected && 'monthpicker-month--selected',
                  ].filter(Boolean).join(' ')}
                  onClick={() => selectMonth(i)}
                  aria-label={`${MONTHS_FULL[i]} ${viewYear}`}
                  aria-pressed={isSelected}
                >
                  {label}
                </button>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}
