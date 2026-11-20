export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export interface LeaveRequest { startDate: string; endDate: string; reason: string | null; }
export interface LeaveResponse extends LeaveRequest {
  id: number; employeeId: string; status: LeaveStatus; validatorUsername: string | null;
  createdAt: string; updatedAt: string;
}
export interface LeaveAssociation { username: string; userId: number; employeeId: string; }
export interface LeaveDirectoryOptions { users: { id: number; label: string }[]; employees: { id: number; label: string }[]; }
