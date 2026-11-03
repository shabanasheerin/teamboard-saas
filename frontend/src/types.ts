export type Role = 'ADMIN' | 'MANAGER' | 'MEMBER';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface UserInfo {
  id: number;
  name: string;
  email: string;
  role: Role;
  active: boolean;
  mustChangePassword: boolean;
  organizationId: number;
  organizationName: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  user: UserInfo;
}

export interface Project {
  id: number;
  name: string;
  key: string;
  description: string | null;
  archived: boolean;
  taskCount: number;
  createdByName: string;
  createdAt: string;
}

export interface Task {
  id: number;
  key: string;
  projectId: number;
  projectName: string;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: Priority;
  dueDate: string | null;
  overdue: boolean;
  assigneeId: number | null;
  assigneeName: string | null;
  reporterId: number;
  reporterName: string;
  createdAt: string;
  updatedAt: string;
}

export interface TaskComment {
  id: number;
  authorId: number;
  authorName: string;
  text: string;
  createdAt: string;
}

export interface AppNotification {
  id: number;
  message: string;
  seen: boolean;
  taskId: number | null;
  projectId: number | null;
  createdAt: string;
}

export interface AuditEntry {
  id: number;
  entityType: string;
  entityId: number | null;
  action: string;
  details: string | null;
  actorName: string;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  totalPages: number;
  totalElements: number;
}

export interface Dashboard {
  total: number;
  todo: number;
  inProgress: number;
  inReview: number;
  done: number;
  overdue: number;
  projects: number;
  members: number;
  openByPriority: Record<string, number>;
}

export interface TaskEvent {
  type: string;
  taskId: number;
  projectId: number;
}

export interface NotificationPush {
  id: number;
  message: string;
  taskId: number | null;
  projectId: number | null;
  createdAt: string;
}
