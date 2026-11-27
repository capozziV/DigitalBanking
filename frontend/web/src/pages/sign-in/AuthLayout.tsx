import { useId, type ComponentProps, type ReactNode } from 'react';
import { cn } from 'cn';
import { AmbientLights } from '@/design/AmbientLights';
import { GlassPanel } from '@/design/GlassPanel';
import { Reveal } from '@/design/motion';
import { Footer } from '@/shell/Footer';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

/**
 * Black field with the ambient lights. The form comes first in the DOM, so on a phone it is what
 * the user meets first; `aside` (the brand side, only for the first impression) follows it.
 * On desktop the row is reversed so the aside stays on the left, side by side with the form.
 */
export function AuthLayout({ children, aside }: { children: ReactNode; aside?: ReactNode }) {
  return (
    <div className="relative isolate flex min-h-dvh flex-col overflow-hidden">
      <AmbientLights intensity={aside ? 'high' : 'low'} />
      <main
        className={cn(
          'relative z-10 mx-auto flex w-full flex-1 flex-col justify-center gap-8 px-5 py-10 md:px-10',
          aside ? 'max-w-5xl lg:flex-row-reverse lg:items-center lg:gap-16' : 'max-w-md',
        )}
      >
        <Reveal delay={0.1} className={aside ? 'w-full lg:max-w-md lg:flex-1' : 'w-full'}>
          <GlassPanel className="flex flex-col gap-5 p-6 md:p-8">{children}</GlassPanel>
        </Reveal>
        {aside ? <div className="flex flex-col gap-6 lg:flex-1">{aside}</div> : null}
      </main>
      <Footer className="relative z-10 px-5 py-6 text-center" />
    </div>
  );
}

export function AuthHeading({ children, brand = true }: { children: string; brand?: boolean }) {
  return (
    <div className="flex flex-col gap-1">
      {brand ? <p className="text-sm font-semibold text-muted-foreground">capzzPay</p> : null}
      <h1 className="font-heading text-3xl leading-tight font-bold tracking-tight">{children}</h1>
    </div>
  );
}

export function FormError({ children }: { children: string | null }) {
  if (!children) return null;
  return (
    <p role="alert" className="rounded-xl bg-negative/10 px-3 py-2 text-sm text-negative">
      {children}
    </p>
  );
}

type FieldProps = Omit<ComponentProps<typeof Input>, 'id'> & {
  label: string;
  error?: string | null;
};

/** A visible label, the input and (when the user already touched it) the message under it. */
export function Field({ label, error, className, ...input }: FieldProps) {
  const id = useId();
  const errorId = `${id}-error`;
  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor={id}>{label}</Label>
      <Input
        id={id}
        className={cn('h-11 rounded-xl bg-glass px-3.5', className)}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        {...input}
      />
      {error ? (
        <p id={errorId} className="text-sm text-negative">
          {error}
        </p>
      ) : null}
    </div>
  );
}

export const SUBMIT_CLASS = 'h-11 w-full rounded-xl text-base font-semibold';
export const isEmail = (value: string): boolean => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
