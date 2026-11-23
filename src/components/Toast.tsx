export interface ToastMessage {
  msg: string;
  id: number;
}

export function Toast({ toast }: { toast: ToastMessage | null }) {
  return (
    <div
      id="toast"
      role="status"
      aria-live="polite"
      className={
        'pointer-events-none fixed bottom-[6.6rem] left-1/2 z-[60] flex max-w-[92vw] -translate-x-1/2 translate-y-3 items-center gap-2 overflow-hidden rounded-full bg-espresso px-4 py-2.5 text-[11.5px] font-bold text-cream shadow-bar' +
        (toast ? ' show' : '')
      }
      style={{ opacity: toast ? undefined : 0 }}
    >
      {toast && (
        <>
          <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-gold-soft" />
          <span className="truncate" style={{ minWidth: 0 }}>
            {toast.msg}
          </span>
        </>
      )}
    </div>
  );
}
