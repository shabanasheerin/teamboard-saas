import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errMsg } from '../api';
import { useAuth } from '../auth';
import { PRIORITIES, fmtDate, statusLabel } from '../constants';
import { onTaskEvent } from '../realtime';
import type { Dashboard as DashboardData, Task } from '../types';

export default function Dashboard() {
  const { user } = useAuth();
  const [stats, setStats] = useState<DashboardData | null>(null);
  const [mine, setMine] = useState<Task[]>([]);
  const [err, setErr] = useState('');

  const load = useCallback(() => {
    Promise.all([api.get<DashboardData>('/dashboard'), api.get<Task[]>('/tasks/mine')])
      .then(([s, t]) => {
        setStats(s.data);
        setMine(t.data);
      })
      .catch((e) => setErr(errMsg(e)));
  }, []);

  useEffect(() => {
    load();
    // any task change in the company refreshes the numbers
    return onTaskEvent(() => load());
  }, [load]);

  if (err) return <p className="error">{err}</p>;
  if (!stats || !user) return <p className="empty">Loading dashboard...</p>;

  const maxOpen = Math.max(1, ...Object.values(stats.openByPriority));
  const donePct = stats.total ? Math.round((stats.done / stats.total) * 100) : 0;

  return (
    <>
      <div className="page-head">
        <h1>Hello, {user.name.split(' ')[0]}</h1>
      </div>

      <div className="stats">
        <div className="stat">
          <span>{stats.total}</span>
          <small>Tasks in {user.organizationName}</small>
        </div>
        <div className="stat">
          <span>{stats.todo}</span>
          <small>To do</small>
        </div>
        <div className="stat">
          <span>{stats.inProgress + stats.inReview}</span>
          <small>In progress or review</small>
        </div>
        <div className="stat good">
          <span>{stats.done}</span>
          <small>Done ({donePct}%)</small>
        </div>
        <div className={`stat ${stats.overdue ? 'bad' : ''}`}>
          <span>{stats.overdue}</span>
          <small>Overdue</small>
        </div>
      </div>

      <div className="two-col">
        <section className="panel">
          <h2>My open tasks</h2>
          {mine.length === 0 && <p className="empty">Nothing assigned to you right now.</p>}
          <ul className="list">
            {mine.slice(0, 8).map((t) => (
              <li key={t.id}>
                <div>
                  <Link to={`/projects/${t.projectId}`}>
                    <strong>
                      {t.key} {t.title}
                    </strong>
                  </Link>
                  <small>
                    {t.projectName} · {statusLabel(t.status)}
                  </small>
                </div>
                <span className={t.overdue ? 'late' : ''}>{t.overdue ? `Overdue ${fmtDate(t.dueDate)}` : fmtDate(t.dueDate)}</span>
              </li>
            ))}
          </ul>
        </section>

        <section className="panel">
          <h2>Open tasks by priority</h2>
          <div className="bars">
            {PRIORITIES.map((p) => (
              <div key={p.key} className="bar-row">
                <span>{p.label}</span>
                <div className="bar-track">
                  <div className={`bar-fill p-${p.key}`} style={{ width: `${((stats.openByPriority[p.key] ?? 0) / maxOpen) * 100}%` }} />
                </div>
                <b>{stats.openByPriority[p.key] ?? 0}</b>
              </div>
            ))}
          </div>
          <p className="muted">
            {stats.projects} active project{stats.projects === 1 ? '' : 's'} · {stats.members} team member
            {stats.members === 1 ? '' : 's'}
          </p>
        </section>
      </div>
    </>
  );
}
