import { ChangeEvent, FormEvent, useEffect, useState } from 'react';
import api, { errMsg } from '../api';
import { useAuth } from '../auth';
import { STATUSES, fmtDate, fmtDateTime, priorityLabel } from '../constants';
import type { Task, TaskComment, TaskStatus } from '../types';
import Modal from './Modal';

interface TaskDetailProps {
  task: Task;
  onClose: () => void;
  onUpdated: (task: Task) => void;
  onEdit: (task: Task) => void;
  onDeleted: (id: number) => void;
}

export default function TaskDetail({ task, onClose, onUpdated, onEdit, onDeleted }: TaskDetailProps) {
  const { user } = useAuth();
  const [comments, setComments] = useState<TaskComment[]>([]);
  const [text, setText] = useState('');
  const [err, setErr] = useState('');

  const manager = user?.role !== 'MEMBER';
  const canEdit = manager || task.reporterId === user?.id;
  const canMove = manager || task.assigneeId === user?.id || task.reporterId === user?.id;

  const loadComments = () =>
    api
      .get<TaskComment[]>(`/tasks/${task.id}/comments`)
      .then((r) => setComments(r.data))
      .catch((e) => setErr(errMsg(e)));

  useEffect(() => {
    void loadComments();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [task.id]);

  const changeStatus = async (e: ChangeEvent<HTMLSelectElement>) => {
    try {
      const { data } = await api.patch<Task>(`/tasks/${task.id}/status`, { status: e.target.value as TaskStatus });
      onUpdated(data);
    } catch (ex) {
      setErr(errMsg(ex));
    }
  };

  const addComment = async (e: FormEvent) => {
    e.preventDefault();
    if (!text.trim()) return;
    try {
      await api.post(`/tasks/${task.id}/comments`, { text });
      setText('');
      void loadComments();
    } catch (ex) {
      setErr(errMsg(ex));
    }
  };

  const remove = async () => {
    if (!window.confirm(`Delete ${task.key}? This cannot be undone.`)) return;
    try {
      await api.delete(`/tasks/${task.id}`);
      onDeleted(task.id);
    } catch (ex) {
      setErr(errMsg(ex));
    }
  };

  return (
    <Modal title={`${task.key}  ${task.title}`} onClose={onClose} wide>
      <div className="meta">
        <div>
          <small>Assignee</small>
          <strong>{task.assigneeName ?? 'Unassigned'}</strong>
        </div>
        <div>
          <small>Reporter</small>
          <strong>{task.reporterName}</strong>
        </div>
        <div>
          <small>Priority</small>
          <strong>{priorityLabel(task.priority)}</strong>
        </div>
        <div>
          <small>Due</small>
          <strong className={task.overdue ? 'late' : ''}>
            {fmtDate(task.dueDate)}
            {task.overdue && ' (overdue)'}
          </strong>
        </div>
        <div>
          <small>Status</small>
          <select value={task.status} onChange={changeStatus} disabled={!canMove}>
            {STATUSES.map((s) => (
              <option key={s.key} value={s.key}>
                {s.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      <p className="desc">{task.description || 'No description added.'}</p>

      <h3>Comments</h3>
      <ul className="comments">
        {comments.length === 0 && <li className="empty">No comments yet. Ask a question or share an update.</li>}
        {comments.map((c) => (
          <li key={c.id}>
            <strong>{c.authorName}</strong> <small>{fmtDateTime(c.createdAt)}</small>
            <p>{c.text}</p>
          </li>
        ))}
      </ul>
      <form onSubmit={addComment} className="comment-form">
        <input value={text} onChange={(e) => setText(e.target.value)} placeholder="Write a comment" maxLength={1000} />
        <button className="btn primary">Post comment</button>
      </form>

      {err && <p className="error">{err}</p>}

      {(canEdit || manager) && (
        <div className="actions">
          {manager && (
            <button className="btn danger" onClick={remove}>
              Delete task
            </button>
          )}
          {canEdit && (
            <button className="btn" onClick={() => onEdit(task)}>
              Edit task
            </button>
          )}
        </div>
      )}
    </Modal>
  );
}
