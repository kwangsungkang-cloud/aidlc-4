export type SseEventHandler = (event: MessageEvent) => void;

export function createSseConnection(
  onEvent: Record<string, SseEventHandler>,
  onError?: (event: Event) => void
): EventSource | null {
  const token = localStorage.getItem('admin_token');
  if (!token) return null;

  const url = `/api/admin/sse/subscribe?token=${encodeURIComponent(token)}`;
  const eventSource = new EventSource(url);

  Object.entries(onEvent).forEach(([eventType, handler]) => {
    eventSource.addEventListener(eventType, handler);
  });

  eventSource.onerror = (e) => {
    if (onError) onError(e);
  };

  return eventSource;
}
