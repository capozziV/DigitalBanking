import { cn } from 'cn';

/**
 * The highlighted words of a headline, in the blue, lilac and pink light. Takes plain text: the
 * drifting layer repeats it from `data-text` in CSS, so the words exist once in the DOM.
 */
export function GradientText({ children, className }: { children: string; className?: string }) {
  return (
    <span className={cn('gradient-text', className)} data-text={children}>
      {children}
    </span>
  );
}
