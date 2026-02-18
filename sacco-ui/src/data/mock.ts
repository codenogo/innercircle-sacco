export type MemberStatus = 'ACTIVE' | 'PENDING' | 'SUSPENDED'

export interface Member {
  id: string
  name: string
  phone: string
  email: string
  status: MemberStatus
  joined: string
  shares: number
}

export const members: Member[] = [
  { id: '1', name: 'Jane Wanjiku',    phone: '0712 345 678', email: 'jane@mail.com',    status: 'ACTIVE',    joined: 'Jan 2024', shares: 45000 },
  { id: '2', name: 'Peter Kamau',     phone: '0723 456 789', email: 'peter@mail.com',   status: 'ACTIVE',    joined: 'Jan 2024', shares: 42000 },
  { id: '3', name: 'Mary Akinyi',     phone: '0734 567 890', email: 'mary@mail.com',    status: 'ACTIVE',    joined: 'Feb 2024', shares: 39000 },
  { id: '4', name: 'John Ochieng',    phone: '0745 678 901', email: 'john@mail.com',    status: 'ACTIVE',    joined: 'Feb 2024', shares: 36000 },
  { id: '5', name: 'Grace Njeri',     phone: '0756 789 012', email: 'grace@mail.com',   status: 'ACTIVE',    joined: 'Mar 2024', shares: 33000 },
  { id: '6', name: 'David Mwangi',    phone: '0767 890 123', email: 'david@mail.com',   status: 'ACTIVE',    joined: 'Mar 2024', shares: 30000 },
  { id: '7', name: 'Sarah Wambui',    phone: '0778 901 234', email: 'sarah@mail.com',   status: 'ACTIVE',    joined: 'Apr 2024', shares: 27000 },
  { id: '8', name: 'James Kiprop',    phone: '0789 012 345', email: 'james@mail.com',   status: 'ACTIVE',    joined: 'Apr 2024', shares: 24000 },
  { id: '9', name: 'Faith Muthoni',   phone: '0790 123 456', email: 'faith@mail.com',   status: 'PENDING',   joined: 'Jan 2026', shares: 0 },
  { id: '10', name: 'Brian Otieno',   phone: '0701 234 567', email: 'brian@mail.com',   status: 'SUSPENDED', joined: 'Jun 2024', shares: 15000 },
]
