const express = require('express');
const http = require('http');
const { WebSocketServer } = require('ws');

const PORT = 8123;
const app = express();
app.use(express.json());

// Bonus: multiple entities support; default entity expected by the mod.
const entities = new Map([
  ['light.living_room', 'off'],
  ['switch.garden', 'off'],
]);

const wsClients = new Set();

function stateObject(entityId) {
  return {
    entity_id: entityId,
    state: entities.get(entityId) || 'off',
  };
}

function log(...args) {
  console.log('[fake-ha]', ...args);
}

function sendStateChanged(entityId) {
  const eventPayload = {
    type: 'event',
    event: {
      event_type: 'state_changed',
      data: {
        entity_id: entityId,
        new_state: {
          state: entities.get(entityId) || 'off',
        },
      },
    },
  };

  for (const client of wsClients) {
    if (
      client.readyState === 1 &&
      client.isAuthed &&
      client.stateChangedSubscriptionId !== null
    ) {
      client.send(
        JSON.stringify({
          id: client.stateChangedSubscriptionId,
          ...eventPayload,
        }),
      );
    }
  }

  log(
    'event sent',
    eventPayload.event.data.entity_id,
    eventPayload.event.data.new_state.state,
  );
}

function toggleEntity(entityId) {
  if (!entities.has(entityId)) {
    entities.set(entityId, 'off');
  }

  const current = entities.get(entityId) || 'off';
  const next = current === 'on' ? 'off' : 'on';
  entities.set(entityId, next);
  sendStateChanged(entityId);
  return next;
}

// Real HA-like endpoint used by many clients/mods.
app.get('/api/states/:entityId', (req, res) => {
  const entityId = decodeURIComponent(req.params.entityId);
  if (!entities.has(entityId)) {
    return res
      .status(404)
      .json({ error: 'entity_not_found', entity_id: entityId });
  }
  res.json(stateObject(entityId));
});

// Optional helper: inspect all entities.
app.get('/api/states', (req, res) => {
  const out = [];
  for (const entityId of entities.keys()) {
    out.push(stateObject(entityId));
  }
  res.json(out);
});

// Toggle requested endpoint; defaults to light.living_room.
app.post('/toggle', (req, res) => {
  const entityId =
    req.body && req.body.entity_id
      ? String(req.body.entity_id)
      : 'light.living_room';

  const next = toggleEntity(entityId);
  res.json({ ok: true, entity_id: entityId, state: next });
});

// HA-compatible service path used by the mod transport layer.
app.post('/api/services/:domain/:service', (req, res) => {
  const domain = String(req.params.domain || '');
  const service = String(req.params.service || '');
  const entityId =
    req.body?.target?.entity_id ||
    req.body?.data?.entity_id ||
    req.body?.entity_id ||
    'light.living_room';

  const normalizedEntityId = String(entityId);
  log('service call', `${domain}.${service}`, normalizedEntityId);

  let next = entities.get(normalizedEntityId) || 'off';
  if (service === 'toggle') {
    next = toggleEntity(normalizedEntityId);
  }

  res.json([
    {
      entity_id: normalizedEntityId,
      state: next,
    },
  ]);
});

const server = http.createServer(app);
const wss = new WebSocketServer({ server, path: '/api/websocket' });

wss.on('connection', (ws, req) => {
  ws.isAuthed = false;
  ws.stateChangedSubscriptionId = null;
  wsClients.add(ws);

  log('ws connect', req.socket.remoteAddress || 'unknown');

  ws.send(JSON.stringify({ type: 'auth_required' }));

  ws.on('message', (raw) => {
    let msg;
    try {
      msg = JSON.parse(raw.toString());
    } catch (err) {
      return;
    }

    if (!ws.isAuthed) {
      if (msg && msg.type === 'auth') {
        ws.isAuthed = true;
        ws.send(JSON.stringify({ type: 'auth_ok' }));
        log('ws auth_ok');
      }
      return;
    }

    // Optional compatibility responses for common HA websocket commands.
    if (msg && msg.type === 'subscribe_events') {
      if (msg.event_type === 'state_changed') {
        ws.stateChangedSubscriptionId = Number.isInteger(msg.id) ? msg.id : 0;
      }
      ws.send(
        JSON.stringify({
          id: msg.id || 0,
          type: 'result',
          success: true,
          result: null,
        }),
      );
      log('ws subscribe_events ok');
      return;
    }

    if (msg && msg.type === 'get_states') {
      const result = [];
      for (const entityId of entities.keys()) {
        result.push(stateObject(entityId));
      }
      ws.send(
        JSON.stringify({
          id: msg.id || 0,
          type: 'result',
          success: true,
          result,
        }),
      );
      log('ws get_states result sent');
      return;
    }
  });

  ws.on('close', () => {
    wsClients.delete(ws);
    log('ws disconnect');
  });

  ws.on('error', (err) => {
    log('ws error', err.message);
  });
});

server.listen(PORT, () => {
  log(`REST + WS listening on http://localhost:${PORT}`);
  log('default entity:', stateObject('light.living_room'));
});
