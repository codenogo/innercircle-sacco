import { useState, useEffect, useRef, type CSSProperties } from 'react'
import { createPortal } from 'react-dom'
import { DotsThree } from '@phosphor-icons/react'
import './ActionMenu.css'

export interface ActionMenuItem {
  label: string
  onClick: () => void
  variant?: 'default' | 'danger'
  disabled?: boolean
}

interface ActionMenuProps {
  actions: ActionMenuItem[]
}

const PANEL_GAP = 4
const VIEWPORT_MARGIN = 8

export function ActionMenu({ actions }: ActionMenuProps) {
  const [open, setOpen] = useState(false)
  const [panelStyle, setPanelStyle] = useState<CSSProperties>({})
  const [panelPlacement, setPanelPlacement] = useState<'top' | 'bottom'>('bottom')

  const triggerRef = useRef<HTMLButtonElement>(null)
  const panelRef = useRef<HTMLDivElement>(null)

  // Close on outside click
  useEffect(() => {
    if (!open) return
    function handleClick(e: MouseEvent) {
      const target = e.target as Node
      if (triggerRef.current?.contains(target)) return
      if (panelRef.current?.contains(target)) return
      setOpen(false)
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [open])

  // Position the panel via portal
  useEffect(() => {
    if (!open) return
    function updatePosition() {
      if (!triggerRef.current) return
      const rect = triggerRef.current.getBoundingClientRect()
      const panelWidth = 160
      const estimatedHeight = actions.length * 34 + 8
      const spaceBelow = window.innerHeight - rect.bottom - VIEWPORT_MARGIN
      const spaceAbove = rect.top - VIEWPORT_MARGIN
      const openAbove = spaceBelow < estimatedHeight && spaceAbove > spaceBelow
      const desiredTop = openAbove
        ? rect.top - estimatedHeight - PANEL_GAP
        : rect.bottom + PANEL_GAP
      const maxTop = Math.max(VIEWPORT_MARGIN, window.innerHeight - estimatedHeight - VIEWPORT_MARGIN)
      const top = Math.min(Math.max(desiredTop, VIEWPORT_MARGIN), maxTop)
      const desiredLeft = rect.right - panelWidth
      const left = Math.min(Math.max(desiredLeft, VIEWPORT_MARGIN), window.innerWidth - panelWidth - VIEWPORT_MARGIN)
      setPanelPlacement(openAbove ? 'top' : 'bottom')
      setPanelStyle({ position: 'fixed', top, left, width: panelWidth, zIndex: 2100 })
    }
    updatePosition()
    window.addEventListener('resize', updatePosition)
    window.addEventListener('scroll', updatePosition, true)
    return () => {
      window.removeEventListener('resize', updatePosition)
      window.removeEventListener('scroll', updatePosition, true)
    }
  }, [open, actions.length])

  // Close on Escape
  useEffect(() => {
    if (!open) return
    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape') setOpen(false)
    }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  }, [open])

  if (actions.length === 0) return null

  return (
    <div className="action-menu">
      <button
        ref={triggerRef}
        type="button"
        className={`action-menu-trigger${open ? ' action-menu-trigger--open' : ''}`}
        onClick={() => setOpen(o => !o)}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label="Row actions"
      >
        <DotsThree size={16} weight="bold" />
      </button>

      {open && createPortal(
        <div
          ref={panelRef}
          className={`action-menu-panel${panelPlacement === 'top' ? ' action-menu-panel--top' : ''}`}
          style={panelStyle}
          role="menu"
        >
          {actions.map((action, i) => (
            <button
              key={i}
              type="button"
              className={`action-menu-item${action.variant === 'danger' ? ' action-menu-item--danger' : ''}`}
              disabled={action.disabled}
              role="menuitem"
              onClick={() => {
                setOpen(false)
                action.onClick()
              }}
            >
              {action.label}
            </button>
          ))}
        </div>,
        document.body,
      )}
    </div>
  )
}

