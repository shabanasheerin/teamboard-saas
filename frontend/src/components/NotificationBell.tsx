import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import { onNotification } from '../realtime';
import { fmtDateTime } from '../constants';
import type { AppNotification } from '../types';

export default function NotificationBell() {
  const [items, setItems] = useState<AppNotification[]>([]);
  const [open, setOpen] = useState(false);
  const box = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  const load = useCallback(() => {
    api
      .get<AppNotification[]>('/notifications')
      .then((r) => setItems(r.data))
      .catch(() => undefined);
  }, []);

  useEffect(() => {
    load();
    // a new notification arrives live over the WebSocket
    return onNotification(() => load());
  }, [load]);

  useEffect(() => {
    const close = (e: MouseEvent) => {
      if (box.current && !box.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  const unread = items.filter((n) => !n.seen).length;

  const openItem = async (n: AppNotification) => {
    if (!n.seen) {
      await api.patch(`/notifications/${n.id}/seen`).catch(() => undefined);
      load();
    }
    if (n.projectId) {
      setOpen(false);
      navigate(`/projects/${n.projectId}`);
    }
  };

  const markAll = async () => {
    await api.patch('/notifications/seen-all').catch(() => undefined);
    load();
  };

  return (
    <div className="bell" ref={box}>
      <button className="btn ghost" onClick={() => setOpen(!open)} aria-label="Notifications">
        Notifications{unread > 0 && <span className="badge">{unread}</span>}
      </button>
      {open && (
        <div className="bell-panel">
          <div className="bell-head">
            <strong>Notifications</strong>
            {unread > 0 && (
              <button className="link" onClick={markAll}>
                Mark all as read
              </button>
            )}
          </div>
          {items.length === 0 && <p className="empty">You are all caught up.</p>}
          <ul>
            {items.map((n) => (
              <li key={n.id} className={n.seen ? '' : 'unread'} onClick={() => openItem(n)}>
                <span>{n.message}</span>
                <small>{fmtDateTime(n.createdAt)}</small>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
