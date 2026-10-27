// Only implemented private routes are accepted. Navigation stays internal.
export function safeReturnUrl(value: string | null | undefined): string {
  if (!value || /[\\\x00-\x20]/.test(value)) return '/home';
  const path = value.split(/[?#]/, 1)[0];
  return /^\/(home|dashboard|trainings|attendances|hr-records|contracts|evaluations|job-offers|applications|leave-calendar|my-evaluations|client|customer-associations|users|profile|hr|my-payroll(?:\/[1-9]\d*)?|(?:employees|customers|invoices|fees|payroll)(?:\/(?:new|[1-9]\d*(?:\/edit)?))?|departments(?:\/(?:new|[1-9]\d*\/edit))?|leaves(?:\/(?:new|[1-9]\d*(?:\/edit)?))?)$/.test(path)
    ? value : '/home';
}
