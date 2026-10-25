// Read-only probes: Department has no public /api/test endpoint.
export async function waitForServices(base, {
  fetchFn = globalThis.fetch, now = Date.now,
  sleep = ms => new Promise(resolve => setTimeout(resolve, ms)), timeoutMs = 90000
} = {}) {
  const deadline = now() + timeoutMs;
  for (const [route, expected] of [['/identity/api/test', 200], ['/department/api/departments', 401]]) {
    let ready = false;
    while (now() < deadline) {
      try {
        const response = await fetchFn(base + route, {
          method: 'GET', redirect: 'error',
          signal: AbortSignal.timeout(Math.max(1, Math.min(2000, deadline - now())))
        });
        await response.body?.cancel();
        ready = response.status === expected;
        if (ready) break;
      } catch { /* Never print response bodies or operational diagnostics. */ }
      const remaining = deadline - now();
      if (remaining > 0) await sleep(Math.min(1000, remaining));
    }
    if (!ready) throw new Error('Gateway service readiness deadline exceeded');
  }
}
