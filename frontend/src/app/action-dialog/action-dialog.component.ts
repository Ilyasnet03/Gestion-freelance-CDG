import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import {
  FormControl,
  FormRecord,
  ReactiveFormsModule,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

export type DialogFieldValue = string | number | null;
export type DialogFieldType =
  | 'text'
  | 'email'
  | 'number'
  | 'date'
  | 'month'
  | 'password'
  | 'select'
  | 'textarea';

export interface DialogFieldOption {
  value: string | number;
  label: string;
}

export interface ActionDialogField {
  key: string;
  label: string;
  type: DialogFieldType;
  required?: boolean;
  visibleWhen?: { key: string; value: string | number };
  min?: number;
  max?: number;
  step?: number;
  hint?: string;
  options?: DialogFieldOption[];
}

export interface ActionDialogData {
  title: string;
  description?: string;
  fields: ActionDialogField[];
  values?: Record<string, DialogFieldValue | undefined>;
  submitLabel?: string;
  danger?: boolean;
}

export type ActionDialogResult = Record<string, DialogFieldValue>;

@Component({
  selector: 'app-action-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './action-dialog.component.html',
  styleUrl: './action-dialog.component.scss',
})
export class ActionDialogComponent {
  readonly data = inject<ActionDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef =
    inject<MatDialogRef<ActionDialogComponent, ActionDialogResult>>(MatDialogRef);
  readonly form = new FormRecord<FormControl<DialogFieldValue>>({});

  constructor() {
    for (const field of this.data.fields) {
      const validators: ValidatorFn[] = [];
      if (field.required) validators.push(Validators.required);
      if (field.type === 'email') validators.push(Validators.email);
      if (field.type === 'number' && field.min !== undefined) {
        validators.push(Validators.min(field.min));
      }
      if (field.type === 'number' && field.max !== undefined) {
        validators.push(Validators.max(field.max));
      }

      this.form.addControl(
        field.key,
        new FormControl<DialogFieldValue>(
          this.data.values?.[field.key] ?? '',
          { validators },
        ),
      );
    }
  }

  control(field: ActionDialogField): FormControl<DialogFieldValue> {
    return this.form.controls[field.key];
  }

  isVisible(field: ActionDialogField): boolean {
    if (!field.visibleWhen) return true;
    return this.form.controls[field.visibleWhen.key]?.value === field.visibleWhen.value;
  }

  hasVisibleErrors(): boolean {
    return this.data.fields.some(
      (field) => this.isVisible(field) && this.control(field).invalid,
    );
  }

  errorText(field: ActionDialogField): string {
    const control = this.control(field);
    if (control.hasError('required')) return 'Ce champ est obligatoire.';
    if (control.hasError('email')) return 'Saisissez une adresse e-mail valide.';
    if (control.hasError('min')) return `La valeur minimale est ${field.min}.`;
    if (control.hasError('max')) return `La valeur maximale est ${field.max}.`;
    return 'Vérifiez cette valeur.';
  }

  submit(): void {
    if (this.hasVisibleErrors()) {
      for (const field of this.data.fields) {
        if (this.isVisible(field)) this.control(field).markAsTouched();
      }
      return;
    }
    this.dialogRef.close(this.form.getRawValue());
  }
}
