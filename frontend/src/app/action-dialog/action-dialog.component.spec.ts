import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {
  ActionDialogComponent,
  ActionDialogData,
} from './action-dialog.component';

describe('ActionDialogComponent', () => {
  const data: ActionDialogData = {
    title: 'Créer un freelance',
    fields: [
      {
        key: 'role',
        label: 'Rôle',
        type: 'select',
        required: true,
        options: [{ value: 'FREELANCE', label: 'Freelance' }, { value: 'ADMIN', label: 'Administrateur' }],
      },
      {
        key: 'tjm',
        label: 'TJM',
        type: 'number',
        min: 0.01,
        visibleWhen: { key: 'role', value: 'FREELANCE' },
      },
    ],
  };
  let close: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    close = vi.fn();
    await TestBed.configureTestingModule({
      imports: [ActionDialogComponent],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: MatDialogRef, useValue: { close } },
      ],
    }).compileComponents();
  });

  it('keeps the dialog open until required values are valid', () => {
    const component = TestBed.createComponent(ActionDialogComponent).componentInstance;

    component.submit();

    expect(close).not.toHaveBeenCalled();
    expect(component.control(data.fields[0]).touched).toBe(true);

    component.control(data.fields[0]).setValue('ADMIN');
    component.submit();

    expect(close).toHaveBeenCalledWith({ role: 'ADMIN', tjm: '' });
  });

  it('validates conditional fields only while they are visible', () => {
    const component = TestBed.createComponent(ActionDialogComponent).componentInstance;
    component.control(data.fields[0]).setValue('FREELANCE');
    component.control(data.fields[1]).setValue(0);

    expect(component.hasVisibleErrors()).toBe(true);

    component.control(data.fields[0]).setValue('ADMIN');

    expect(component.hasVisibleErrors()).toBe(false);
    component.submit();
    expect(close).toHaveBeenCalledWith({ role: 'ADMIN', tjm: 0 });
  });
});
