import { BreakpointObserver } from '@angular/cdk/layout';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  OnInit,
  signal,
  viewChild,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';
import { SessionService } from '../core/session.service';

/** Side menu: fixed from 960 px up, a sliding drawer (hamburger) below. */
@Component({
  selector: 'app-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'escape()' },
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss',
})
export class AppShellComponent implements OnInit {
  protected readonly session = inject(SessionService);

  protected readonly wide = toSignal(
    inject(BreakpointObserver)
      .observe('(min-width: 960px)')
      .pipe(map((state) => state.matches)),
    { initialValue: true },
  );
  protected readonly open = signal(false);
  private readonly hamburger = viewChild<ElementRef<HTMLButtonElement>>('hamburger');
  private readonly nav = viewChild<ElementRef<HTMLElement>>('nav');

  ngOnInit(): void {
    void this.session.refreshUnread();
  }

  protected close(): void {
    this.open.set(false);
  }

  protected toggle(): void {
    this.open.update((value) => !value);
    if (this.open()) {
      setTimeout(() => this.nav()?.nativeElement.querySelector('a')?.focus());
    }
  }

  protected escape(): void {
    if (!this.open() || this.wide()) return;
    this.close();
    this.hamburger()?.nativeElement.focus();
  }

  protected signOut(): void {
    this.close();
    void this.session.signOut();
  }
}
