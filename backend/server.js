import express from 'express';
import http from 'http';
import { WebSocketServer, WebSocket } from 'ws';
import cors from 'cors';

const app = express();
app.use(cors());
app.use(express.json({ limit: '50mb' }));

const server = http.createServer(app);
const wss = new WebSocketServer({ server, path: '/ws' });

const PORT = process.env.PORT || 3000;

// Connected clients
const clients = new Set();

// Heartbeat interval to clean up dead connections
const HEARTBEAT_INTERVAL_MS = 30000;
const heartbeat = setInterval(() => {
  for (const ws of clients) {
    if (ws.isAlive === false) {
      console.log('[Flippy Radio] Terminando conexión muerta');
      ws.terminate();
      clients.delete(ws);
      continue;
    }
    ws.isAlive = false;
    ws.ping();
  }
}, HEARTBEAT_INTERVAL_MS);

heartbeat.unref?.();

app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    app: 'Flippy Walkie-Talkie Native Backend',
    version: '1.0.0',
    clientsConnected: clients.size,
    timestamp: Date.now()
  });
});

wss.on('connection', (ws) => {
  clients.add(ws);
  ws.isAlive = true;
  console.log(`[Flippy Radio] Cliente conectado. Total activos: ${clients.size}`);

  ws.on('pong', () => {
    ws.isAlive = true;
  });

  ws.on('message', (messageBuffer) => {
    try {
      const dataStr = messageBuffer.toString();
      const data = JSON.parse(dataStr);

      // Handle ping for latency measurement
      if (data.type === 'ping') {
        ws.send(JSON.stringify({ type: 'pong', time: data.time }));
        return;
      }

      // Broadcast PTT Start, PTT End, and Voice Messages to all other connected clients
      clients.forEach((client) => {
        if (client !== ws && client.readyState === WebSocket.OPEN) {
          client.send(dataStr);
        }
      });
    } catch (e) {
      console.error('[Flippy Radio] Error procesando mensaje:', e);
    }
  });

  ws.on('close', () => {
    clients.delete(ws);
    console.log(`[Flippy Radio] Cliente desconectado. Total activos: ${clients.size}`);
  });

  ws.on('error', (err) => {
    console.error('[Flippy Radio] Socket error:', err);
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`📡 Flippy Radio WebSocket Server running at http://0.0.0.0:${PORT} / ws://0.0.0.0:${PORT}/ws`);
});
