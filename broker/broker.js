import http from "node:http";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PORTA = Number(process.argv[2] || process.env.BROKER_PORTA || 9000);
const DIR_DADOS = path.join(__dirname, "dados-broker");
const ARQ_LOG = path.join(DIR_DADOS, "log.ndjson");
const ARQ_OFFSETS = path.join(DIR_DADOS, "offsets.json");
const ESPERA_LONGPOLL_MS = 25000;

fs.mkdirSync(DIR_DADOS, { recursive: true });

let log = [];
let offsets = {};
let proximoSeq = 1;
const espera = [];

function carregar() {
  if (fs.existsSync(ARQ_LOG)) {
    const linhas = fs.readFileSync(ARQ_LOG, "utf8").split("\n").filter(Boolean);
    log = linhas.map((l) => JSON.parse(l));
    if (log.length > 0) proximoSeq = log[log.length - 1].seq + 1;
  }
  if (fs.existsSync(ARQ_OFFSETS)) {
    offsets = JSON.parse(fs.readFileSync(ARQ_OFFSETS, "utf8"));
  }
  console.log(`[broker] estado recuperado: ${log.length} eventos no log, proximo seq=${proximoSeq}`);
  console.log(`[broker] offsets dos grupos:`, offsets);
}

function persistirEvento(evento) {
  fs.appendFileSync(ARQ_LOG, JSON.stringify(evento) + "\n");
}

function persistirOffsets() {
  fs.writeFileSync(ARQ_OFFSETS, JSON.stringify(offsets, null, 2));
}

function corresponde(topicoEvento, topicosAssinados) {
  if (topicosAssinados.includes("#")) return true;
  return topicosAssinados.includes(topicoEvento);
}

function coletar(grupo, topicos, max) {
  const desde = offsets[grupo] || 0;
  const selecionados = [];
  let proximoOffset = desde;
  for (const ev of log) {
    if (ev.seq <= desde) continue;
    proximoOffset = ev.seq;
    if (corresponde(ev.topico, topicos)) {
      selecionados.push(ev);
      if (selecionados.length >= max) break;
    }
  }
  return { eventos: selecionados, proximoOffset };
}

function notificarEsperas() {
  while (espera.length > 0) {
    const w = espera.shift();
    if (w.respondido) continue;
    const r = coletar(w.grupo, w.topicos, w.max);
    if (r.eventos.length > 0) {
      w.respondido = true;
      clearTimeout(w.timer);
      responderJson(w.res, 200, r);
    } else {
      espera.push(w);
      break;
    }
  }
}

function responderJson(res, status, corpo) {
  const texto = JSON.stringify(corpo);
  res.writeHead(status, { "Content-Type": "application/json" });
  res.end(texto);
}

function lerCorpo(req) {
  return new Promise((resolve, reject) => {
    let dados = "";
    req.on("data", (c) => (dados += c));
    req.on("end", () => resolve(dados));
    req.on("error", reject);
  });
}

const servidor = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://localhost:${PORTA}`);

  if (req.method === "POST" && url.pathname === "/publicar") {
    const corpo = JSON.parse((await lerCorpo(req)) || "{}");
    if (!corpo.topico) {
      return responderJson(res, 400, { ok: false, erro: "topico ausente" });
    }
    const evento = {
      seq: proximoSeq++,
      topico: corpo.topico,
      dados: corpo.dados || {},
      ts: new Date().toISOString(),
    };
    log.push(evento);
    persistirEvento(evento);
    console.log(`[broker] <- publicado seq=${evento.seq} topico="${evento.topico}"`);
    notificarEsperas();
    return responderJson(res, 201, { ok: true, seq: evento.seq });
  }

  if (req.method === "GET" && url.pathname === "/consumir") {
    const grupo = url.searchParams.get("grupo");
    const topicos = (url.searchParams.get("topicos") || "#").split(",").map((t) => t.trim());
    const max = Number(url.searchParams.get("max") || 50);
    if (!grupo) {
      return responderJson(res, 400, { ok: false, erro: "grupo ausente" });
    }
    const r = coletar(grupo, topicos, max);
    if (r.eventos.length > 0) {
      return responderJson(res, 200, r);
    }
    const w = { grupo, topicos, max, res, respondido: false };
    w.timer = setTimeout(() => {
      if (w.respondido) return;
      w.respondido = true;
      const idx = espera.indexOf(w);
      if (idx >= 0) espera.splice(idx, 1);
      responderJson(res, 200, { eventos: [], proximoOffset: offsets[grupo] || 0 });
    }, ESPERA_LONGPOLL_MS);
    espera.push(w);
    return;
  }

  if (req.method === "POST" && url.pathname === "/confirmar") {
    const corpo = JSON.parse((await lerCorpo(req)) || "{}");
    if (!corpo.grupo || corpo.offset === undefined) {
      return responderJson(res, 400, { ok: false, erro: "grupo ou offset ausente" });
    }
    const atual = offsets[corpo.grupo] || 0;
    offsets[corpo.grupo] = Math.max(atual, corpo.offset);
    persistirOffsets();
    console.log(`[broker] -> grupo "${corpo.grupo}" confirmou offset=${offsets[corpo.grupo]}`);
    return responderJson(res, 200, { ok: true, offset: offsets[corpo.grupo] });
  }

  if (req.method === "GET" && url.pathname === "/status") {
    const porTopico = {};
    for (const ev of log) porTopico[ev.topico] = (porTopico[ev.topico] || 0) + 1;
    return responderJson(res, 200, {
      totalEventos: log.length,
      proximoSeq,
      eventosPorTopico: porTopico,
      offsets,
      esperandoLongPoll: espera.length,
    });
  }

  responderJson(res, 404, { ok: false, erro: "rota nao encontrada" });
});

carregar();
servidor.listen(PORTA, () => {
  console.log(`[broker] pub-sub no ar em http://localhost:${PORTA}`);
  console.log(`[broker] dados persistidos em ${DIR_DADOS}`);
});
