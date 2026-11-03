import { useEffect, useState } from 'react';
import api, { errMsg } from '../api';
import { fmtDateTime } from '../constants';
import type { AuditEntry, PageResponse } from '../types';

export default function Audit() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageResponse<AuditEntry> | null>(null);
  const [err, setErr] = useState('');

  useEffect(() => {
    api
      .get<PageResponse<AuditEntry>>('/audit', { params: { page, size: 30 } })
      .then((r) => setData(r.data))
      .catch((e) => setErr(errMsg(e)));
  }, [page]);

  return (
    <>
      <div className="page-head">
        <h1>Activity log</h1>
      </div>
      {err && <p className="error">{err}</p>}
      {data && data.content.length === 0 && <p className="empty">No activity yet.</p>}
      {data && data.content.length > 0 && (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>When</th>
                  <th>Who</th>
                  <th>What</th>
                  <th>Details</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((r) => (
                  <tr key={r.id}>
                    <td>{fmtDateTime(r.createdAt)}</td>
                    <td>{r.actorName}</td>
                    <td>{r.action.replace(/_/g, ' ').toLowerCase()}</td>
                    <td>{r.details}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="pager">
            <button className="btn" disabled={page === 0} onClick={() => setPage(page - 1)}>
              Previous
            </button>
            <span>
              Page {data.page + 1} of {Math.max(data.totalPages, 1)}
            </span>
            <button className="btn" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>
              Next
            </button>
          </div>
        </>
      )}
    </>
  );
}
