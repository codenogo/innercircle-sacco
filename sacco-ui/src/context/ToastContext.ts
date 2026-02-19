import { createContext } from 'react'

export interface ToastAPI {
  success: (title: string, message?: string, duration?: number) => void
  error: (title: string, message?: string, duration?: number) => void
  warning: (title: string, message?: string, duration?: number) => void
  info: (title: string, message?: string, duration?: number) => void
  dismiss: (id: string) => void
}

export const ToastContext = createContext<ToastAPI | null>(null)
