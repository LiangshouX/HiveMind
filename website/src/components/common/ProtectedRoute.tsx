import { Navigate, useLocation } from "react-router-dom";
import { Spin } from "antd";
import { useAuth } from "../../providers/AuthProvider";

export function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { authenticated, ready } = useAuth();
  const location = useLocation();

  if (!ready) {
    return (
      <div className="route-loading-shell">
        <Spin size="large" />
      </div>
    );
  }

  if (!authenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return <>{children}</>;
}
