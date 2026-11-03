import { FormEvent, useState } from 'react';
import api, { errMsg } from '../api';
import { PRIORITIES } from '../constants';
import type { Priority, Task, UserInfo } from '../types';
import Modal from './Modal';

interface TaskFormProps {
  projectId: number;
  task?: Task;
  people: UserInfo[];
  onClose: () => void;
  onSaved: () => void;
}

export default function TaskForm({ projectId, task, people, onClose, onSaved }: TaskFormProps) {
  const editing = Boolean(task);
  const [title, setTitle] = useState(task?.title ?? '');
  const [description, setDescription] = useState(task?.description ?? '');
  const [priority, setPriority] = useState<Priority>(task?.priority ?? 'MEDIUM');
  const [dueDate, setDueDate] = useState(task?.dueDate ?? '');
  const [assigneeId, setAssigneeId] = useState<string>(task?.assigneeId ? String(task.assigneeId) : '');
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setErr('');
    const body = {
      title,
      description,
      priority,
      dueDate: dueDate || null,
      assigneeId: assigneeId ? Number(assigneeId) : null,
    };
    try {
      if (task) await api.put(`/tasks/${task.id}`, body);
      else await api.post(`/projects/${projectId}/tasks`, body);
      onSaved();
    } catch (ex) {
      setErr(errMsg(ex));
      setBusy(false);
    }
  };

  return (
    <Modal title={editing ? `Edit ${task?.key}` : 'New task'} onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <label>
          Title
          <input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={200} required autoFocus />
        </label>
        <label>
          Description
          <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={4} maxLength={4000} />
        </label>
        <div className="row">
          <label>
            Assign to
            <select value={assigneeId} onChange={(e) => setAssigneeId(e.target.value)}>
              <option value="">Unassigned</option>
              {people.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Priority
            <select value={priority} onChange={(e) => setPriority(e.target.value as Priority)}>
              {PRIORITIES.map((p) => (
                <option key={p.key} value={p.key}>
                  {p.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            Due date
            <input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
          </label>
        </div>
        {err && <p className="error">{err}</p>}
        <div className="actions">
          <button type="button" className="btn" onClick={onClose}>
            Cancel
          </button>
          <button className="btn primary" disabled={busy}>
            {editing ? 'Save changes' : 'Create task'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
