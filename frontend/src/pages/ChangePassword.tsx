import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api, { errMsg } from '../api';
import { useAuth } from '../auth';

export default function ChangePassword() {
  const { user, updateUser, logout } = useAuth();
  const navigate = useNavigate();
  const forced = Boolean(user?.mustChangePassword);
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirm, setConfirm] = useState('');
  const [err, setErr] = useState('');
  const [done, setDone] = useState(false);
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setErr('');
    if (next !== confirm) {
      setErr('The new passwords do not match.');
      return;
    }
    setBusy(true);
    try {
      await api.post('/users/me/password', { currentPassword: current, newPassword: next });
      updateUser({ mustChangePassword: false });
      setDone(true);
      setTimeout(() => navigate('/'), 900);
    } catch (ex) {
      setErr(errMsg(ex));
      setBusy(false);
    }
  };

  return (
    <div className="login single">
      <form className="login-card" onSubmit={submit}>
        <h2>{forced ? 'Choose a new password' : 'Change password'}</h2>
        {forced && <p>Your account uses a temporary password. Set your own to continue.</p>}
        <label>
          {forced ? 'Temporary password' : 'Current password'}
          <input type="password" value={current} onChange={(e) => setCurrent(e.target.value)} required autoFocus />
        </label>
        <label>
          New password (8+ characters, with a letter and a number)
          <input type="password" value={next} onChange={(e) => setNext(e.target.value)} minLength={8} required />
        </label>
        <label>
          Confirm new password
          <input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} minLength={8} required />
        </label>
        {err && <p className="error">{err}</p>}
        {done && <p className="ok">Password changed. Taking you in...</p>}
        <button className="btn primary full" disabled={busy}>
          Save new password
        </button>
        {forced ? (
          <button
            type="button"
            className="link"
            onClick={async () => {
              await logout();
              navigate('/login');
            }}
          >
            Log out
          </button>
        ) : (
          <Link to="/" className="link">
            Back to dashboard
          </Link>
        )}
      </form>
    </div>
  );
}
