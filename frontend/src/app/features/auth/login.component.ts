import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth.service';
import { UniverseApiService } from '../universe/universe-api.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './auth-form.css'
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly universeApi = inject(UniverseApiService);

  protected readonly errorMessage = signal<string | null>(null);
  protected readonly submitting = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      return;
    }
    this.errorMessage.set(null);
    this.submitting.set(true);
    const { username, password } = this.form.getRawValue();
    this.auth.login(username, password).subscribe({
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
        this.errorMessage.set('Invalid username or password.');
      }
    });
  }
}
