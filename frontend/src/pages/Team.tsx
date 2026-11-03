import { FormEvent, useCallback, useEffect, useState } from 'react';
import api, { errMsg } from '../api';
import { useAuth } from '../auth';
import Modal from '../components/Modal';
import { ROLE_LABEL } from '../constants';
import type { Role, UserInfo } from '../types';

export default function Team() {
  const { user: me } = useAuth();
  const isAdmin = me?.role === 'ADMIN';
  const [people, setPeople] = useState<UserInfo[]>([]);
  const [err, setErr] = useState('');
  const [adding, setAdding] = useState(false);
  const [resetFor, setResetFor] = useState<UserInfo | null>(null);

  // add person form
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<Role>('MEMBER');
  const [formErr, setFormErr] = useState('');
  // reset form
  const [newPw, setNewPw] = useState('');
  const [resetErr, setResetErr] = useState('');

  const load = useCallback(() => {
    api
      .get<UserInfo[]>('/users')
      .then((r) => setPeople(r.data))
      .catch((e) => setErr(errMsg(e)));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const create = async (e: FormEvent) => {
    e.preventDefault();
    setFormErr('');
    try {
      await api.post('/users', { name, email, password, role });
      setAdding(false);
      setName('');
      setEmail('');
      setPassword('');
      setRole('MEMBER');
      load();
    } catch (ex) {
      setFormErr(errMsg(ex));
    }
  };

  const changeRole = async (p: UserInfo, next: Role) => {
    try {
      await api.patch(`/users/${p.id}/role`, { role: next });
      load();
    } catch (ex) {
      setErr(errMsg(ex));
    }
  };

  const toggle = async (p: UserInfo) => {
    try {
      await api.patch(`/users/${p.id}/active`);
      load();
    } catch (ex) {
      setErr(errMsg(ex));
    }
  };

  const reset = async (e: FormEvent) => {
    e.preventDefault();
    if (!resetFor) return;
    setResetErr('');
    try {
      await api.patch(`/users/${resetFor.id}/password`, { newPassword: newPw });
      setResetFor(null);
      setNewPw('');
    } catch (ex) {
      setResetErr(errMsg(ex));
    }
  };

  return (
    <>
      <div className="page-head">
        <h1>Team</h1>
        {isAdmin && (
          <button className="btn primary" onClick={() => setAdding(true)}>
            Add person
          </button>
        )}
      </div>
      {err && <p className="error">{err}</p>}

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
              {isAdmin && <th>Status</th>}
              {isAdmin && <th></th>}
            </tr>
          </thead>
          <tbody>
            {people.map((p) => (
              <tr key={p.id}>
                <td>{p.name}</td>
                <td>{p.email}</td>
                <td>
                  {isAdmin && p.id !== me?.id ? (
                    <select value={p.role} onChange={(e) => void changeRole(p, e.target.value as Role)}>
                      {(Object.keys(ROLE_LABEL) as Role[]).map((r) => (
                        <option key={r} value={r}>
                          {ROLE_LABEL[r]}
                        </option>
                      ))}
                    </select>
                  ) : (
                    ROLE_LABEL[p.role]
                  )}
                </td>
                {isAdmin && <td>{p.active ? 'Active' : 'Deactivated'}</td>}
                {isAdmin && (
                  <td>
                    <button className="link" onClick={() => setResetFor(p)}>
                      Reset password
                    </button>
                    {p.id !== me?.id && (
                      <>
                        {' · '}
                        <button className="link" onClick={() => void toggle(p)}>
                          {p.active ? 'Deactivate' : 'Activate'}
                        </button>
                      </>
                    )}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {adding && (
        <Modal title="Add person" onClose={() => setAdding(false)}>
          <form className="form" onSubmit={create}>
            <label>
              Full name
              <input value={name} onChange={(e) => setName(e.target.value)} required autoFocus />
            </label>
            <label>
              Work email
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
            </label>
            <label>
              Temporary password (8+ characters, with a letter and a number)
              <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} minLength={8} required />
            </label>
            <label>
              Role
              <select value={role} onChange={(e) => setRole(e.target.value as Role)}>
                {(Object.keys(ROLE_LABEL) as Role[]).map((r) => (
                  <option key={r} value={r}>
                    {ROLE_LABEL[r]}
                  </option>
                ))}
              </select>
            </label>
            <small>They must choose their own password the first time they log in.</small>
            {formErr && <p className="error">{formErr}</p>}
            <div className="actions">
              <button type="button" className="btn" onClick={() => setAdding(false)}>
                Cancel
              </button>
              <button className="btn primary">Add person</button>
            </div>
          </form>
        </Modal>
      )}

      {resetFor && (
        <Modal title={`Reset password for ${resetFor.name}`} onClose={() => setResetFor(null)}>
          <form className="form" onSubmit={reset}>
            <label>
              Temporary password (8+ characters, with a letter and a number)
              <input type="password" value={newPw} onChange={(e) => setNewPw(e.target.value)} minLength={8} required autoFocus />
            </label>
            <small>They will be signed out everywhere and must choose a new password at next login.</small>
            {resetErr && <p className="error">{resetErr}</p>}
            <div className="actions">
              <button type="button" className="btn" onClick={() => setResetFor(null)}>
                Cancel
              </button>
              <button className="btn primary">Set temporary password</button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
