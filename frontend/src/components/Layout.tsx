import { useEffect } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth';
import { ROLE_LABEL } from '../constants';
import { connectRealtime, disconnectRealtime } from '../realtime';
import NotificationBell from './NotificationBell';

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  // one live connection while the person is logged in
  useEffect(() => {
    if (!user) return;
    connectRealtime(user.organizationId);
    return () => disconnectRealtime();
  }, [user?.organizationId]); // eslint-disable-line react-hooks/exhaustive-deps

  if (!user) return null;
  const canSeeActivity = user.role !== 'MEMBER';

  return (
    <div className="shell">
      <aside className="sidebar">
        <div>
          <div className="brand">TeamBoard</div>
          <div className="org">{user.organizationName}</div>
        </div>
        <nav>
          <NavLink to="/" end>
            Dashboard
          </NavLink>
          <NavLink to="/projects">Projects</NavLink>
          <NavLink to="/team">Team</NavLink>
          {canSeeActivity && <NavLink to="/activity">Activity log</NavLink>}
        </nav>
        <div className="me">
          <strong>{user.name}</strong>
          <span>{ROLE_LABEL[user.role]}</span>
          <NavLink to="/change-password" className="link light">
            Change password
          </NavLink>
          <button
            className="link light"
            onClick={async () => {
              await logout();
              navigate('/login');
            }}
          >
            Log out
          </button>
        </div>
      </aside>
      <div className="main">
        <div className="topbar">
          <NotificationBell />
        </div>
        <main>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
