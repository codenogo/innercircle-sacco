import { SpinnerGap } from '@phosphor-icons/react'

interface SpinnerProps {
  size?: 'sm' | 'md' | 'lg'
  muted?: boolean
}

const SIZES = { sm: 14, md: 20, lg: 28 } as const

export function Spinner({ size = 'md', muted }: SpinnerProps) {
  return (
    <span className={`spinner spinner--${size}${muted ? ' spinner--muted' : ''}`}>
      <SpinnerGap size={SIZES[size]} weight="bold" />
    </span>
  )
}
