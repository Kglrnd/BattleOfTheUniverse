import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';

import { AuthService } from '../../core/auth.service';
import { UniverseApiService } from '../universe/universe-api.service';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, TranslocoDirective],
  templateUrl: './register.component.html',
  styleUrl: './auth-form.css'
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly universeApi = inject(UniverseApiService);
  private readonly transloco = inject(TranslocoService);

  protected readonly errorMessage = signal<string | null>(null);
  protected readonly submitting = signal(false);
  // Defaults to open so a failed/slow status check never blocks a real registration attempt -
  // the backend still enforces the actual gate, this is purely so players aren't asked to fill
  // out a form that's doomed to fail.
  protected readonly registrationOpen = signal(true);

  protected readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  constructor() {
    this.auth.registrationStatus().subscribe({
      next: (status) => this.registrationOpen.set(status.registrationEnabled),
      error: () => this.registrationOpen.set(true)
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      return;
    }
    this.errorMessage.set(null);
    this.submitting.set(true);
    const request = { ...this.form.getRawValue(), preferredLanguage: this.transloco.getActiveLang() };
    this.auth.register(request).subscribe({
      next: () => {
        this.auth.login(request.username, request.password).subscribe({
          next: () => {
            this.universeApi.getHomePlanet().subscribe({
              next: (planet) => {
                this.submitting.set(false);
                this.router.navigate(['/universe', planet.id]);
              },
              error: () => {
                this.submitting.set(false);
                this.router.navigate(['/universe']);
              }
            });
          },
          error: () => {
            this.submitting.set(false);
            this.router.navigate(['/login']);
          }
        });
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        this.errorMessage.set(err.error?.message ?? this.transloco.translate('auth.register.registrationFailed'));
      }
    });
  }
}
