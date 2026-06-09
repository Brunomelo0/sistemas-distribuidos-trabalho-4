import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { ClienteBroker } from "./clienteBroker.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const urlBroker = process.argv[2] || process.env.BROKER_URL || "http://localhost:9000";
const DIR_DADOS = path.join(__dirname, "dados-auditoria");
const ARQ_AUDITORIA = path.join(DIR_DADOS, "auditoria.log");

fs.mkdirSync(DIR_DADOS, { recursive: true });

const cliente = new ClienteBroker(urlBroker, "auditoria", ["#"]);
let total = 0;

function processar(evento) {
  total++;
  const linha = `${evento.ts} seq=${evento.seq} ${evento.topico} ${JSON.stringify(evento.dados)}`;
  fs.appendFileSync(ARQ_AUDITORIA, linha + "\n");
  console.log(`[auditoria] (total ${total}) registrado: ${evento.topico}`);
}

console.log(`[auditoria] assinando todos os topicos ("#") no broker ${urlBroker}`);
console.log(`[auditoria] gravando em ${ARQ_AUDITORIA}`);
cliente.iniciar(processar);
