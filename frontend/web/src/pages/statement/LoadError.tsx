import { Button } from '@/components/ui/button';

/** Discreet notice for a load that failed (BFF down, network), with a retry button. */
export function LoadError({ onRetry }: { onRetry: () => void }) {
  return (
    <p
      role="status"
      className="mb-4 flex flex-wrap items-center gap-3 rounded-2xl border border-line px-4 py-3 text-sm text-muted-foreground"
    >
      <span>Não foi possível carregar. Tente de novo.</span>
      <Button type="button" variant="outline" size="sm" onClick={onRetry}>
        Tentar de novo
      </Button>
    </p>
  );
}
