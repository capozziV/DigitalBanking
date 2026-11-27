/** Discreet notice for a load that failed (BFF down, network), with a retry button. */
export function LoadNotice({ onRetry }: { onRetry: () => void }) {
  return (
    <p role="status" className="text-sm text-muted-foreground">
      Não foi possível carregar. Tente de novo.{' '}
      <button
        type="button"
        onClick={onRetry}
        className="font-semibold text-foreground underline underline-offset-2"
      >
        Tentar de novo
      </button>
    </p>
  );
}
