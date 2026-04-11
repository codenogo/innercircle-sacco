import { useState, useEffect, useRef, type CSSProperties, type KeyboardEvent as ReactKeyboardEvent } from 'react'
import { createPortal } from 'react-dom'
import { CaretDown, Check, MagnifyingGlass } from '@phosphor-icons/react'
import './Select.css'

interface SelectOption {
  value: string
  label: string
}

interface SelectProps {
  value: string
  onChange: (value: string) => void
  options: SelectOption[]
  placeholder?: string
  required?: boolean
  searchable?: boolean
  disabled?: boolean
}

const PANEL_GAP = 4
const VIEWPORT_MARGIN = 8

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max)
}

export function Select({ value, onChange, options, placeholder = 'Select...', required, searchable, disabled }: SelectProps) {
  const [open, setOpen] = useState(false)
  const [search, setSearch] = useState('')
  const [highlightIndex, setHighlightIndex] = useState(-1)
  const [panelStyle, setPanelStyle] = useState<CSSProperties>({})
  const [panelPlacement, setPanelPlacement] = useState<'top' | 'bottom'>('bottom')

  const wrapRef = useRef<HTMLDivElement>(null)
  const triggerRef = useRef<HTMLButtonElement>(null)
  const panelRef = useRef<HTMLDivElement>(null)
  const searchRef = useRef<HTMLInputElement>(null)
  const optionsRef = useRef<HTMLDivElement>(null)

  const selected = options.find(o => o.value === value)

  const filtered = searchable && search
    ? options.filter(o => o.label.toLowerCase().includes(search.toLowerCase()))
    : options

  useEffect(() => {
    if (!open) return
    function handleClick(e: MouseEvent) {
      const target = e.target as Node
      if (wrapRef.current?.contains(target)) return
      if (panelRef.current?.contains(target)) return
      setOpen(false)
      setSearch('')
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [open])

  useEffect(() => {
    if (!open) return

    function updatePanelPosition() {
      if (!triggerRef.current) return

      const triggerRect = triggerRef.current.getBoundingClientRect()
      const panelWidth = Math.max(160, triggerRect.width)
      const estimatedHeight = searchable ? 280 : 220
      const spaceBelow = window.innerHeight - triggerRect.bottom - VIEWPORT_MARGIN
      const spaceAbove = triggerRect.top - VIEWPORT_MARGIN
      const openAbove = spaceBelow < estimatedHeight && spaceAbove > spaceBelow

      const desiredTop = openAbove
        ? triggerRect.top - estimatedHeight - PANEL_GAP
        : triggerRect.bottom + PANEL_GAP
      const maxTop = Math.max(VIEWPORT_MARGIN, window.innerHeight - estimatedHeight - VIEWPORT_MARGIN)
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
  }, [open, searchable])

  useEffect(() => {
    if (!open) return
    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        setOpen(false)
        setSearch('')
      }
    }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  }, [open])

  useEffect(() => {
    if (open && searchable && searchRef.current) {
      searchRef.current.focus()
    }
  }, [open, searchable])

  useEffect(() => {
    setHighlightIndex(-1)
  }, [search])

  useEffect(() => {
    if (!disabled) return
    setOpen(false)
    setSearch('')
  }, [disabled])

  useEffect(() => {
    if (highlightIndex < 0 || !optionsRef.current) return
    const el = optionsRef.current.children[highlightIndex] as HTMLElement
    if (el) el.scrollIntoView({ block: 'nearest' })
  }, [highlightIndex])

  function handleSelect(val: string) {
    onChange(val)
    setOpen(false)
    setSearch('')
  }

  function handleKeyDown(e: ReactKeyboardEvent) {
    if (!open) return
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setHighlightIndex(i => Math.min(i + 1, filtered.length - 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setHighlightIndex(i => Math.max(i - 1, 0))
    } else if (e.key === 'Enter' && highlightIndex >= 0 && highlightIndex < filtered.length) {
      e.preventDefault()
      handleSelect(filtered[highlightIndex].value)
    }
  }

  return (
    <div className="select" ref={wrapRef}>
      <button
        ref={triggerRef}
        type="button"
        className={`select-trigger ${open ? 'select-trigger--open' : ''} ${!value ? 'select-trigger--placeholder' : ''}`}
        onClick={() => setOpen(o => !o)}
        aria-haspopup="listbox"
        aria-expanded={open}
        disabled={disabled}
      >
        <span className="select-trigger-text">
          {selected ? selected.label : placeholder}
        </span>
        <CaretDown size={14} weight="bold" className={`select-chevron ${open ? 'select-chevron--open' : ''}`} />
      </button>

      {required && (
        <input
          type="text"
          required
          value={value}
          tabIndex={-1}
          aria-hidden="true"
          style={{ position: 'absolute', opacity: 0, height: 0, width: 0, pointerEvents: 'none' }}
          onChange={() => {}}
        />
      )}

      {open && createPortal(
        <div
          ref={panelRef}
          className={`select-panel ${panelPlacement === 'top' ? 'select-panel--top' : ''}`}
          style={panelStyle}
          role="listbox"
          onKeyDown={handleKeyDown}
        >
          {searchable && (
            <div className="select-search-wrap">
              <MagnifyingGlass size={12} weight="bold" className="select-search-icon" />
              <input
                ref={searchRef}
                type="text"
                className="select-search"
                placeholder="Search..."
                value={search}
                onChange={e => setSearch(e.target.value)}
              />
            </div>
          )}
          <div className="select-options" ref={optionsRef}>
            {filtered.length === 0 ? (
              <div className="select-empty">No results</div>
            ) : (
              filtered.map((o, i) => (
                <button
                  key={o.value}
                  type="button"
                  className={[
                    'select-option',
                    o.value === value && 'select-option--selected',
                    i === highlightIndex && 'select-option--highlight',
                  ].filter(Boolean).join(' ')}
                  onClick={() => handleSelect(o.value)}
                  role="option"
                  aria-selected={o.value === value}
                >
                  <span className="select-option-label">{o.label}</span>
                  {o.value === value && <Check size={14} weight="bold" className="select-option-check" />}
                </button>
              ))
            )}
          </div>
        </div>,
        document.body,
      )}
    </div>
  )
}
