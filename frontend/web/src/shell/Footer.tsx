import { cn } from 'cn';

export function Footer({ className }: { className?: string }) {
  return (
    <footer className={cn('text-xs text-muted-foreground', className)}>© 2026 capzz.labs</footer>
  );
}
