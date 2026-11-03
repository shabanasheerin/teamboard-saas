import { FormEvent, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { errMsg } from '../api';
import { useAuth } from '../auth';

/*const DEMO = [
  { label: 'Acme admin', email: 'admin@acme.com', password: 'Admin@123' },
  { label: 'Acme manager', email: 'manager@acme.com', password: 'Manager@123' },
  { label: 'Acme member', email: 'ravi@acme.com', password: 'Member@123' },
  { label: 'Globex admin', email: 'admin@globex.com', password: 'Admin@123' },
];
*/
export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/" replace />;

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setErr('');
    try {
      const u = await login(email, password);
      navigate(u.mustChangePassword ? '/change-password' : '/');
    } catch (ex) {
      setErr(errMsg(ex));
      setBusy(false);
    }
  };

  return (
    <div className="login">
      <div className="login-intro">
        <h1>TeamBoard</h1>
        <p>One private workspace for every company. Plan projects, move tasks, and see changes live.</p>
      </div>
      <form className="login-card" onSubmit={submit}>
        <h2>Log in</h2>
        <label>
          Work email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        {err && <p className="error">{err}</p>}
        <button className="btn primary full" disabled={busy}>
          {busy ? 'Logging in...' : 'Log in'}
        </button>
        <p className="muted">
          New company? <Link to="/register">Create a workspace</Link>
        </p>
       
      </form>
    </div>
  );
}
