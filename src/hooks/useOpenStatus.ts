import { useEffect, useState } from 'react';

export interface OpenStatus {
  open: boolean;
  text: string;
}

export const SCHEDULE_TEXT = 'Abierto: 7:30 AM – 12:00 PM y 3:30 PM – 7:00 PM';

export function computeStatus(): OpenStatus {
  let minutes: number;
  try {
    const parts = new Intl.DateTimeFormat('en-GB', {
      timeZone: 'America/Lima',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    }).formatToParts(new Date());
    const h = (+(parts.find((p) => p.type === 'hour')?.value ?? 0) || 0) % 24;
    const m = +(parts.find((p) => p.type === 'minute')?.value ?? 0) || 0;
    minutes = h * 60 + m;
  } catch {
    const d = new Date();
    minutes = d.getHours() * 60 + d.getMinutes();
  }

  const M1 = 450; // 7:30
  const M2 = 720; // 12:00
  const M3 = 930; // 15:30
  const M4 = 1140; // 19:00

  if ((minutes >= M1 && minutes < M2) || (minutes >= M3 && minutes < M4)) {
    return { open: true, text: 'Abierto ahora' };
  }
  if (minutes < M1) return { open: false, text: 'Abre 7:30 AM' };
  if (minutes >= M2 && minutes < M3) return { open: false, text: 'Abre 3:30 PM' };
  return { open: false, text: 'Cerrado' };
}

export function useOpenStatus(): OpenStatus {
  const [status, setStatus] = useState<OpenStatus>(computeStatus);
  useEffect(() => {
    const id = setInterval(() => setStatus(computeStatus()), 60 * 1000);
    return () => clearInterval(id);
  }, []);
  return status;
}
