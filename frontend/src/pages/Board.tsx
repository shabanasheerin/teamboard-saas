import { DragEvent, useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { errMsg } from '../api';
import { PRIORITIES, STATUSES, fmtDate, initials } from '../constants';
import { onTaskEvent } from '../realtime';
import TaskDetail from '../components/TaskDetail';
import TaskForm from '../components/TaskForm';
import type { Priority, Project, Task, TaskStatus, UserInfo } from '../types';

export default function Board() {
  const projectId = Number(useParams().id);
  const [project, setProject] = useState<Project | null>(null);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [people, setPeople] = useState<UserInfo[]>([]);
  const [err, setErr] = useState('');
  const [q, setQ] = useState('');
  const [prio, setPrio] = useState<Priority | ''>('');
  const [open, setOpen] = useState<Task | null>(null);
  const [form, setForm] = useState<{ task?: Task } | null>(null);
  const [dragOver, setDragOver] = useState<TaskStatus | ''>('');

  const load = useCallback(async () => {
    try {
      const [p, t] = await Promise.all([
        api.get<Project>(`/projects/${projectId}`),
        api.get<Task[]>(`/projects/${projectId}/tasks`),
      ]);
      setProject(p.data);
      setTasks(t.data);
      // keep an open task window in sync
      setOpen((current) => (current ? t.data.find((x) => x.id === current.id) ?? null : null));
      setErr('');
    } catch (e) {
      setErr(errMsg(e));
    }
  }, [projectId]);

  useEffect(() => {
    void load();
    api
      .get<UserInfo[]>('/users')
      .then((r) => setPeople(r.data.filter((u) => u.active)))
      .catch(() => undefined);
  }, [load]);

  // live updates: when anyone in the company changes a task in this project, reload
  useEffect(() => onTaskEvent((e) => e.projectId === projectId && void load()), [projectId, load]);

  const move = async (id: number, status: TaskStatus) => {
    const previous = tasks;
    setTasks((ts) => ts.map((t) => (t.id === id ? { ...t, status, overdue: t.overdue && status !== 'DONE' } : t)));
    try {
      await api.patch(`/tasks/${id}/status`, { status });
    } catch (e) {
      setTasks(previous);
      setErr(errMsg(e));
    }
  };

  const onDrop = (e: DragEvent, status: TaskStatus) => {
    e.preventDefault();
    setDragOver('');
    const id = Number(e.dataTransfer.getData('text/plain'));
    const task = tasks.find((t) => t.id === id);
    if (task && task.status !== status) void move(id, status);
  };

  const shown = tasks.filter(
    (t) =>
      (!prio || t.priority === prio) &&
      (!q ||
        t.title.toLowerCase().includes(q.toLowerCase()) ||
        t.key.toLowerCase().includes(q.toLowerCase()) ||
        (t.assigneeName ?? '').toLowerCase().includes(q.toLowerCase()))
  );

  if (!project && err) return <p className="error">{err}</p>;
  if (!project) return <p className="empty">Loading board...</p>;

  return (
    <>
      <div className="page-head">
        <div>
          <Link to="/projects" className="link">
            Projects
          </Link>
          <h1>
            {project.name} <span className="key">{project.key}</span>
          </h1>
        </div>
        {!project.archived && (
          <button className="btn primary" onClick={() => setForm({})}>
            New task
          </button>
        )}
      </div>
      {project.archived && <p className="error">This project is archived. Restore it to add tasks.</p>}

      <div className="filters">
        <input placeholder="Search by task, key or person" value={q} onChange={(e) => setQ(e.target.value)} />
        <select value={prio} onChange={(e) => setPrio(e.target.value as Priority | '')}>
          <option value="">All priorities</option>
          {PRIORITIES.map((p) => (
            <option key={p.key} value={p.key}>
              {p.label}
            </option>
          ))}
        </select>
      </div>

      {err && <p className="error">{err}</p>}

      <div className="board four">
        {STATUSES.map((col) => {
          const items = shown.filter((t) => t.status === col.key);
          return (
            <section
              key={col.key}
              className={`column ${dragOver === col.key ? 'over' : ''}`}
              onDragOver={(e) => {
                e.preventDefault();
                setDragOver(col.key);
              }}
              onDragLeave={() => setDragOver('')}
              onDrop={(e) => onDrop(e, col.key)}
            >
              <h2>
                {col.label} <span className="count">{items.length}</span>
              </h2>
              {items.length === 0 && <p className="empty">Nothing here. Drag a task in.</p>}
              {items.map((t) => (
                <article
                  key={t.id}
                  draggable
                  className={`task p-${t.priority}`}
                  onDragStart={(e) => e.dataTransfer.setData('text/plain', String(t.id))}
                  onClick={() => setOpen(t)}
                >
                  <small className="task-key">{t.key}</small>
                  <strong>{t.title}</strong>
                  <div className="task-meta">
                    <span className={`due ${t.overdue ? 'late' : ''}`}>
                      {t.overdue ? 'Overdue ' : ''}
                      {fmtDate(t.dueDate)}
                    </span>
                    <span className="avatar" title={t.assigneeName ?? 'Unassigned'}>
                      {t.assigneeName ? initials(t.assigneeName) : '–'}
                    </span>
                  </div>
                </article>
              ))}
            </section>
          );
        })}
      </div>

      {open && (
        <TaskDetail
          task={open}
          onClose={() => setOpen(null)}
          onUpdated={(updated) => {
            setTasks((ts) => ts.map((t) => (t.id === updated.id ? updated : t)));
            setOpen(updated);
          }}
          onEdit={(t) => {
            setOpen(null);
            setForm({ task: t });
          }}
          onDeleted={(id) => {
            setTasks((ts) => ts.filter((t) => t.id !== id));
            setOpen(null);
          }}
        />
      )}
      {form && (
        <TaskForm
          projectId={projectId}
          task={form.task}
          people={people}
          onClose={() => setForm(null)}
          onSaved={() => {
            setForm(null);
            void load();
          }}
        />
      )}
    </>
  );
}
