import { useState, useEffect, useRef } from 'react'
import { ChevronDown, Check, Search } from 'lucide-react'
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
}

export function Select({ value, onChange, options, placeholder = 'Select\u2026', required, searchable }: SelectProps) {
  const [open, setOpen] = useState(false)
  const [search, setSearch] = useState('')
  const [highlightIndex, setHighlightIndex] = useState(-1)
  const wrapRef = useRef<HTMLDivElement>(null)
  const searchRef = useRef<HTMLInputElement>(null)
  const optionsRef = useRef<HTMLDivElement>(null)

  const selected = options.find(o => o.value === value)

  const filtered = searchable && search
    ? options.filter(o => o.label.toLowerCase().includes(search.toLowerCase()))
    : options

  // Close on outside click
  useEffect(() => {
    if (!open) return
    function handleClick(e: MouseEvent) {
      if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) {
        setOpen(false)
        setSearch('')
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [open])

  // Close on Escape
  useEffect(() => {
    if (!open) return
    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape') { setOpen(false); setSearch('') }
    }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  }, [open])

  // Focus search input when opening
  useEffect(() => {
    if (open && searchable && searchRef.current) {
      searchRef.current.focus()
    }
  }, [open, searchable])

  // Reset highlight when search changes
  useEffect(() => {
    setHighlightIndex(-1)
  }, [search])

  // Scroll highlighted option into view
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

  function handleKeyDown(e: React.KeyboardEvent) {
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
    <div className="select" ref={wrapRef} onKeyDown={handleKeyDown}>
      <button
        type="button"
        className={`select-trigger ${open ? 'select-trigger--open' : ''} ${!value ? 'select-trigger--placeholder' : ''}`}
        onClick={() => setOpen(o => !o)}
        aria-haspopup="listbox"
        aria-expanded={open}
      >
        <span className="select-trigger-text">
          {selected ? selected.label : placeholder}
        </span>
        <ChevronDown size={14} strokeWidth={2} className={`select-chevron ${open ? 'select-chevron--open' : ''}`} />
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

      {open && (
        <div className="select-panel" role="listbox">
          {searchable && (
            <div className="select-search-wrap">
              <Search size={12} strokeWidth={2} className="select-search-icon" />
              <input
                ref={searchRef}
                type="text"
                className="select-search"
                placeholder="Search\u2026"
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
                  {o.value === value && <Check size={14} strokeWidth={2} className="select-option-check" />}
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  )
}
