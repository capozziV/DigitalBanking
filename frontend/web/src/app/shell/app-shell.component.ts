import { BreakpointObserver } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';
import { SessionService } from '../core/session.service';

/** Side menu: fixed from 960 px up, a sliding drawer (hamburger) below. */
@Component({
  selector: 'app-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
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

  ngOnInit(): void {
    void this.session.refreshUnread();
  }

  protected close(): void {
    this.open.set(false);
  }

  protected toggle(): void {
    this.open.update((value) => !value);
  }

  protected signOut(): void {
    this.close();
    void this.session.signOut();
  }
}
