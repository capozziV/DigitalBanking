import type { ComponentPropsWithoutRef, ElementType, ReactNode } from 'react';
import { cn } from 'cn';

type GlassPanelProps<T extends ElementType> = {
  as?: T;
  className?: string;
  children?: ReactNode;
} & Omit<ComponentPropsWithoutRef<T>, 'as' | 'className' | 'children'>;

/** Frosted glass: the one material for balance, entries and notifications. Never nest it. */
export function GlassPanel<T extends ElementType = 'div'>({
  as,
  className,
  children,
  ...rest
}: GlassPanelProps<T>) {
  const Element: ElementType = as ?? 'div';
  return (
    <Element className={cn('glass-panel', className)} {...rest}>
      {children}
    </Element>
  );
}
