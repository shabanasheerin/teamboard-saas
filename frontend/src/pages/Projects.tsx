import { FormEvent, useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errMsg } from '../api';
import { useAuth } from '../auth';
import Modal from '../components/Modal';
import type { Project } from '../types';

export default function Projects() {
  const { user } = useAuth();
  const canManage = user?.role !== 'MEMBER';
  const [projects, setProjects] = useState<Project[]>([]);
  const [err, setErr] = useState('');
  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const [key, setKey] = useState('');
  const [description, setDescription] = useState('');
  const [formErr, setFormErr] = useState('');

  const load = useCallback(() => {
    api
      .get<Project[]>('/projects')
      .then((r) => setProjects(r.data))
      .catch((e) => setErr(errMsg(e)));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const create = async (e: FormEvent) => {
    e.preventDefault();
    setFormErr('');
    try {
      await api.post('/projects', { name, key, description });
      setOpen(false);
      setName('');
      setKey('');
      setDescription('');
      load();
    } catch (ex) {
      setFormErr(errMsg(ex));
    }
  };

  const toggleArchive = async (p: Project) => {
    try {
      await api.patch(`/projects/${p.id}/archive`);
      load();
    } catch (ex) {
      setErr(errMsg(ex));
    }
  };

  return (
    <>
      <div className="page-head">
        <h1>Projects</h1>
        {canManage && (
          <button className="btn primary" onClick={() => setOpen(true)}>
            New project
          </button>
        )}
      </div>
      {err && <p className="error">{err}</p>}
      {projects.length === 0 && !err && (
        <p className="empty">{canManage ? 'No projects yet. Create your first one.' : 'No projects yet. A manager will add one.'}</p>
      )}

      <div className="cards">
        {projects.map((p) => (
          <article key={p.id} className={`card ${p.archived ? 'archived' : ''}`}>
            <div className="card-top">
              <span className="key">{p.key}</span>
              {p.archived && <span className="tag">Archived</span>}
            </div>
            <h2>
              <Link to={`/projects/${p.id}`}>{p.name}</Link>
            </h2>
            <p>{p.description || 'No description.'}</p>
            <div className="card-foot">
              <small>
                {p.taskCount} task{p.taskCount === 1 ? '' : 's'} · by {p.createdByName}
              </small>
              {canManage && (
                <button className="link" onClick={() => toggleArchive(p)}>
                  {p.archived ? 'Restore' : 'Archive'}
                </button>
              )}
            </div>
          </article>
        ))}
      </div>

      {open && (
        <Modal title="New project" onClose={() => setOpen(false)}>
          <form className="form" onSubmit={create}>
            <label>
              Project name
              <input value={name} onChange={(e) => setName(e.target.value)} maxLength={120} required autoFocus />
            </label>
            <label>
              Short key (2 to 10 letters or digits, for example WEB)
              <input value={key} onChange={(e) => setKey(e.target.value.toUpperCase())} maxLength={10} required />
            </label>
            <label>
              Description
              <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={3} maxLength={1000} />
            </label>
            {formErr && <p className="error">{formErr}</p>}
            <div className="actions">
              <button type="button" className="btn" onClick={() => setOpen(false)}>
                Cancel
              </button>
              <button className="btn primary">Create project</button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
