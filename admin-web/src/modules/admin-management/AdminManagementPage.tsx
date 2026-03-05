import { useState } from 'react';
import { Button } from '@/common/components/ui/button';
import { Input } from '@/common/components/ui/input';
import { Card, CardHeader, CardTitle, CardContent } from '@/common/components/ui/card';
import { Badge } from '@/common/components/ui/badge';
import { api } from '@/common/lib/api-client';
import type { AdminUser, AuditLog, PageResponse } from '@/common/types';

export default function AdminManagementPage() {
  const [storeId, setStoreId] = useState('');
  const [admins, setAdmins] = useState<AdminUser[]>([]);
  const [storeName, setStoreName] = useState('');
  const [newUsername, setNewUsername] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [showAudit, setShowAudit] = useState(false);
  const [auditPage, setAuditPage] = useState(0);
  const [auditTotal, setAuditTotal] = useState(0);

  const fetchAdmins = async () => {
    if (!storeId) return;
    try {
      const data = await api.get<{ storeId: number; storeName: string; admins: AdminUser[]; totalCount: number }>(
        `/super-admin/stores/${storeId}/admins`
      );
      setAdmins(data.admins);
      setStoreName(data.storeName);
    } catch (err: any) { alert(err.message); }
  };

  const handleCreateAdmin = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.post('/super-admin/admins', {
        storeId: parseInt(storeId),
        username: newUsername,
        password: newPassword,
      });
      setNewUsername('');
      setNewPassword('');
      fetchAdmins();
    } catch (err: any) { alert(err.message); }
  };

  const handleDeleteAdmin = async (adminId: number) => {
    if (!confirm('이 관리자를 삭제하시겠습니까?')) return;
    try {
      await api.delete(`/super-admin/admins/${adminId}`);
      fetchAdmins();
    } catch (err: any) { alert(err.message); }
  };

  const fetchAuditLogs = async (page = 0) => {
    try {
      const data = await api.get<PageResponse<AuditLog>>(`/super-admin/audit-logs?page=${page}&size=20`);
      setAuditLogs(data.content);
      setAuditPage(data.page);
      setAuditTotal(data.totalPages);
      setShowAudit(true);
    } catch (err: any) { alert(err.message); }
  };

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-semibold">관리자 계정 관리</h2>
        <Button variant="outline" onClick={() => fetchAuditLogs()}>감사 이력</Button>
      </div>

      <Card className="mb-6">
        <CardContent className="p-4">
          <div className="flex gap-3 items-end">
            <div>
              <label className="text-sm font-medium">매장 ID</label>
              <Input type="number" min="1" value={storeId} onChange={(e) => setStoreId(e.target.value)} placeholder="매장 ID 입력" />
            </div>
            <Button onClick={fetchAdmins}>조회</Button>
          </div>
        </CardContent>
      </Card>

      {storeName && (
        <>
          <h3 className="font-medium mb-3">{storeName} 관리자 목록</h3>

          <Card className="mb-4">
            <CardContent className="p-4">
              <form onSubmit={handleCreateAdmin} className="flex gap-3 items-end">
                <div>
                  <label className="text-sm font-medium">사용자명</label>
                  <Input value={newUsername} onChange={(e) => setNewUsername(e.target.value)} required pattern="^[a-zA-Z0-9]+$" />
                </div>
                <div>
                  <label className="text-sm font-medium">비밀번호</label>
                  <Input type="password" minLength={8} value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required />
                </div>
                <Button type="submit">관리자 추가</Button>
              </form>
            </CardContent>
          </Card>

          <div className="space-y-2">
            {admins.map((admin) => (
              <Card key={admin.adminId}>
                <CardContent className="p-4 flex justify-between items-center">
                  <div>
                    <span className="font-medium">{admin.username}</span>
                    {admin.locked && <Badge variant="destructive" className="ml-2">잠금</Badge>}
                    <span className="text-sm text-muted-foreground ml-3">
                      생성: {new Date(admin.createdAt).toLocaleDateString('ko-KR')}
                    </span>
                  </div>
                  <Button size="sm" variant="destructive" onClick={() => handleDeleteAdmin(admin.adminId)}>삭제</Button>
                </CardContent>
              </Card>
            ))}
            {admins.length === 0 && <p className="text-sm text-muted-foreground">등록된 관리자가 없습니다</p>}
          </div>
        </>
      )}

      {showAudit && (
        <Card className="mt-6">
          <CardHeader>
            <div className="flex justify-between items-center">
              <CardTitle className="text-base">감사 이력</CardTitle>
              <Button size="sm" variant="ghost" onClick={() => setShowAudit(false)}>닫기</Button>
            </div>
          </CardHeader>
          <CardContent>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b">
                    <th className="text-left p-2">일시</th>
                    <th className="text-left p-2">수행자</th>
                    <th className="text-left p-2">액션</th>
                    <th className="text-left p-2">대상</th>
                    <th className="text-left p-2">매장</th>
                  </tr>
                </thead>
                <tbody>
                  {auditLogs.map((log) => (
                    <tr key={log.logId} className="border-b">
                      <td className="p-2">{new Date(log.performedAt).toLocaleString('ko-KR')}</td>
                      <td className="p-2">{log.performerUsername}</td>
                      <td className="p-2">
                        <Badge variant={log.actionType === 'CREATED' ? 'success' : 'destructive'}>{log.actionType}</Badge>
                      </td>
                      <td className="p-2">{log.targetUsername}</td>
                      <td className="p-2">{log.storeName}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {auditTotal > 1 && (
              <div className="flex gap-2 mt-4 justify-center">
                <Button size="sm" variant="outline" disabled={auditPage === 0} onClick={() => fetchAuditLogs(auditPage - 1)}>이전</Button>
                <span className="text-sm py-2">{auditPage + 1} / {auditTotal}</span>
                <Button size="sm" variant="outline" disabled={auditPage >= auditTotal - 1} onClick={() => fetchAuditLogs(auditPage + 1)}>다음</Button>
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
