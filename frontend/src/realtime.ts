import { Client } from '@stomp/stompjs';
import { ensureFreshToken } from './api';
import type { NotificationPush, TaskEvent } from './types';

/**
 * One WebSocket (STOMP) connection for the whole app.
 *  - /user/queue/notifications : private notifications for this person
 *  - /topic/org/{id}/tasks     : task changes in this person's company (boards refresh live)
 */
const taskListeners = new Set<(e: TaskEvent) => void>();
const notificationListeners = new Set<(n: NotificationPush) => void>();
let client: Client | null = null;

export function connectRealtime(orgId: number) {
  if (client) return;
  const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws';
  const c: Client = new Client({
    brokerURL: `${scheme}://${window.location.host}/ws`,
    reconnectDelay: 5000,
    beforeConnect: async () => {
      const token = await ensureFreshToken();
      c.connectHeaders = { Authorization: `Bearer ${token ?? ''}` };
    },
    onConnect: () => {
      c.subscribe(`/topic/org/${orgId}/tasks`, (m) => {
        const event = JSON.parse(m.body) as TaskEvent;
        taskListeners.forEach((l) => l(event));
      });
      c.subscribe('/user/queue/notifications', (m) => {
        const push = JSON.parse(m.body) as NotificationPush;
        notificationListeners.forEach((l) => l(push));
      });
    },
  });
  client = c;
  c.activate();
}

export function disconnectRealtime() {
  if (client) {
    void client.deactivate();
    client = null;
  }
}

/** Returns a function that removes the listener (use it as a React effect cleanup). */
export function onTaskEvent(listener: (e: TaskEvent) => void): () => void {
  taskListeners.add(listener);
  return () => {
    taskListeners.delete(listener);
  };
}

export function onNotification(listener: (n: NotificationPush) => void): () => void {
  notificationListeners.add(listener);
  return () => {
    notificationListeners.delete(listener);
  };
}
