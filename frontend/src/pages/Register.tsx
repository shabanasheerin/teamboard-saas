import { FormEvent, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { errMsg } from '../api';
import { useAuth } from '../auth';

export default function Register() {
  const { user, register } = useAuth();
  const navigate = useNavigate();
  const [organizationName, setOrganizationName] = useState('');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/" replace />;

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setErr('');
    if (password !== confirm) {
      setErr('The passwords do not match.');
      return;
    }
    setBusy(true);
    try {
      await register(organizationName, name, email, password);
      navigate('/');
    } catch (ex) {
      setErr(errMsg(ex));
      setBusy(false);
    }
  };

  return (
    <div className="login">
      <div className="login-intro">
        <h1>TeamBoard</h1>
        <p>Create a private workspace for your company. You become its admin and can add your team next.</p>
      </div>
      <form className="login-card" onSubmit={submit}>
        <h2>Create your workspace</h2>
        <label>
          Company name
          <input value={organizationName} onChange={(e) => setOrganizationName(e.target.value)} maxLength={100} required autoFocus />
        </label>
        <label>
          Your name
          <input value={name} onChange={(e) => setName(e.target.value)} maxLength={100} required />
        </label>
        <label>
          Work email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Password (8+ characters, with a letter and a number)
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} minLength={8} required />
        </label>
        <label>
          Confirm password
          <input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} minLength={8} required />
        </label>
        {err && <p className="error">{err}</p>}
        <button className="btn primary full" disabled={busy}>
          {busy ? 'Creating...' : 'Create workspace'}
        </button>
        <p className="muted">
          Already have an account? <Link to="/login">Log in</Link>
        </p>
      </form>
    </div>
  );
}
