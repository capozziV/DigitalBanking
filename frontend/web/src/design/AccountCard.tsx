import { usePrefersReducedMotion } from '@/design/motion';
import { accountTypeLabel, type AccountType } from '@/shell/session-stub';

interface AccountCardProps {
  type: AccountType;
  last4?: string;
  size?: 'sm' | 'md' | 'lg';
  still?: boolean;
}

/**
 * The signature piece: a tilted card that floats, with a light sweeping across it. The finish
 * follows the account type. It stays still when asked or when the visitor reduces motion.
 */
export function AccountCard({ type, last4, size = 'md', still = false }: AccountCardProps) {
  const reduced = usePrefersReducedMotion();
  const label = accountTypeLabel(type);
  return (
    <div
      role="img"
      aria-label={`Cartão ${label}`}
      className="account-card"
      data-finish={type.toLowerCase()}
      data-size={size}
      data-still={String(still || reduced)}
    >
      <div className="account-card__brand">capzzPay</div>
      <div className="account-card__type">{label}</div>
      <div className="account-card__chip" />
      {last4 ? <div className="account-card__number">•••• {last4}</div> : null}
    </div>
  );
}
