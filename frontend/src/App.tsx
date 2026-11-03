import { ReactNode } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth';
import type { Role } from './types';
import Layout from './components/Layout';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Projects from './pages/Projects';
import Board from './pages/Board';
import Team from './pages/Team';
import Audit from './pages/Audit';
import ChangePassword from './pages/ChangePassword';

interface ProtectedProps {
  roles?: Role[];
  allowTemporary?: boolean;
  children: ReactNode;
}

function Protected({ roles, allowTemporary, children }: ProtectedProps) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (user.mustChangePassword && !allowTemporary) return <Navigate to="/change-password" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route
        path="/change-password"
        element={
          <Protected allowTemporary>
            <ChangePassword />
          </Protected>
        }
      />
      <Route
        element={
          <Protected>
            <Layout />
          </Protected>
        }
      >
        <Route index element={<Dashboard />} />
        <Route path="projects" element={<Projects />} />
        <Route path="projects/:id" element={<Board />} />
        <Route path="team" element={<Team />} />
        <Route
          path="activity"
          element={
            <Protected roles={['ADMIN', 'MANAGER']}>
              <Audit />
            </Protected>
          }
        />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
