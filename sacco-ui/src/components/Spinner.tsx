import { Loader2 } from 'lucide-react'

interface SpinnerProps {
  size?: 'sm' | 'md' | 'lg'
  muted?: boolean
}

const SIZES = { sm: 14, md: 20, lg: 28 } as const

export function Spinner({ size = 'md', muted }: SpinnerProps) {
  return (
    <span className={`spinner spinner--${size}${muted ? ' spinner--muted' : ''}`}>
      <Loader2 size={SIZES[size]} strokeWidth={2} />
    </span>
  )
}
