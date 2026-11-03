import type { Priority, Role, TaskStatus } from './types';

export const STATUSES: { key: TaskStatus; label: string }[] = [
  { key: 'TODO', label: 'To do' },
  { key: 'IN_PROGRESS', label: 'In progress' },
  { key: 'IN_REVIEW', label: 'In review' },
  { key: 'DONE', label: 'Done' },
];

export const PRIORITIES: { key: Priority; label: string }[] = [
  { key: 'LOW', label: 'Low' },
  { key: 'MEDIUM', label: 'Medium' },
  { key: 'HIGH', label: 'High' },
  { key: 'URGENT', label: 'Urgent' },
];

export const ROLE_LABEL: Record<Role, string> = { ADMIN: 'Admin', MANAGER: 'Manager', MEMBER: 'Member' };

export const statusLabel = (key: string) => STATUSES.find((s) => s.key === key)?.label ?? key;
export const priorityLabel = (key: string) => PRIORITIES.find((p) => p.key === key)?.label ?? key;

export const fmtDate = (d: string | null) =>
  d ? new Date(d).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : 'No due date';

export const fmtDateTime = (d: string) =>
  new Date(d).toLocaleString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });

export const initials = (name: string | null) =>
  (name ?? '?')
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join('');
