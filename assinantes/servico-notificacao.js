import { ClienteBroker } from "./clienteBroker.js";

const urlBroker = process.argv[2] || process.env.BROKER_URL || "http://localhost:9000";
const cliente = new ClienteBroker(urlBroker, "notificacao", ["aluno.matriculado", "folha.emitida"]);

function processar(evento) {
  const d = evento.dados;
  if (evento.topico === "aluno.matriculado") {
    console.log(`[notificacao] (seq ${evento.seq}) Bem-vindo(a), ${d.alunoNome}! Matricula confirmada em "${d.curso}".`);
  } else if (evento.topico === "folha.emitida") {
    console.log(`[notificacao] (seq ${evento.seq}) ${d.alunoNome}, sua bolsa de R$ ${Number(d.pagamento).toFixed(2)} foi processada.`);
  }
}

console.log(`[notificacao] assinando ["aluno.matriculado","folha.emitida"] no broker ${urlBroker}`);
cliente.iniciar(processar);
