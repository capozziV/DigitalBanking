import {
  ArrowUpRight,
  Bell,
  CircleUserRound,
  House,
  ReceiptText,
  type LucideIcon,
} from 'lucide-react';

export interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
}

/** The five destinations of a customer, in the order of the side navigation and the tab bar. */
export const NAV_ITEMS: readonly NavItem[] = [
  { to: '/inicio', label: 'Início', icon: House },
  { to: '/extrato', label: 'Extrato', icon: ReceiptText },
  { to: '/transferir', label: 'Transferir', icon: ArrowUpRight },
  { to: '/notificacoes', label: 'Notificações', icon: Bell },
  { to: '/minha-conta', label: 'Minha conta', icon: CircleUserRound },
];

/** The badge text: the count up to 99, then "99+". */
export const unreadBadge = (count: number): string => (count > 99 ? '99+' : String(count));
