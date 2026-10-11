export function Loading({ label = "Loading..." }: { label?: string }) {
  return <div className="muted pad" role="status">{label}</div>;
}

export function ErrorNote({ error }: { error: unknown }) {
  const msg = error instanceof Error ? error.message : String(error);
  return <div className="error-note" role="alert">{msg}</div>;
}

export function Empty({ children }: { children: React.ReactNode }) {
  return <div className="muted pad">{children}</div>;
}
