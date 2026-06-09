import { ApiServico } from "./apiServico.js";

function cabecalho(titulo) {
  console.log();
  console.log(`=== ${titulo} ===`);
}

function descreverAluno(a) {
  return `${a.tipo}[id=${a.id}, nome=${a.nome}, matricula=${a.matricula}]`;
}

async function main() {
  const baseUrl = process.argv[2] || process.env.SERVIDOR_URL || "http://localhost:8080";
  const api = new ApiServico(baseUrl);
  console.log(`[cliente-js] conectado em ${baseUrl}`);

  try {
    cabecalho('1) criarDepartamento("DComp - Computacao")');
    const dComp = await api.criarDepartamento("DComp - Computacao");
    console.log(`    -> retorno: ${dComp.id} (${dComp.nome})`);

    cabecalho('2) criarCurso("Ciencia da Computacao", DComp)');
    const cc = await api.criarCurso(dComp.id, "Ciencia da Computacao");
    console.log(`    -> retorno: ${cc.id} (${cc.nomeCurso})`);

    cabecalho('3) criarCurso("Sistemas de Informacao", DComp)');
    const si = await api.criarCurso(dComp.id, "Sistemas de Informacao");
    console.log(`    -> retorno: ${si.id} (${si.nomeCurso})`);

    cabecalho("4) matricularAluno(AlunoGraduacao Ana, CC)");
    const ana = { tipo: "AlunoGraduacao", id: 1, nome: "Ana Silva", matricula: "2024001" };
    console.log(`    aluno (cliente): ${descreverAluno(ana)}`);
    await api.matricularAluno(cc.id, ana);

    cabecalho("5) matricularAluno(AlunoIC Bruno, CC)");
    const bruno = { tipo: "AlunoIC", id: 2, nome: "Bruno Costa", matricula: "2024002", diasTrabalhados: 20, valorBolsa: 700.0 };
    console.log(`    aluno (cliente): ${descreverAluno(bruno)}`);
    await api.matricularAluno(cc.id, bruno);

    cabecalho("6) matricularAluno(AlunoPosGraduacao Carla, SI)");
    const carla = { tipo: "AlunoPosGraduacao", id: 3, nome: "Carla Lima", matricula: "2024003", diasTrabalhados: 30, valorBolsa: 2200.0 };
    console.log(`    aluna (cliente): ${descreverAluno(carla)}`);
    await api.matricularAluno(si.id, carla);

    cabecalho("7) emitirFolhaPagamento(Bruno)");
    const folhaBruno = await api.emitirFolhaPagamento(bruno);
    console.log(`    -> retorno: R$ ${folhaBruno.pagamento.toFixed(2)}`);

    cabecalho("8) emitirFolhaPagamento(Carla)");
    const folhaCarla = await api.emitirFolhaPagamento(carla);
    console.log(`    -> retorno: R$ ${folhaCarla.pagamento.toFixed(2)}`);

    cabecalho("9) listarAlunosCurso(CC)");
    const alunosCC = await api.listarAlunosCurso(cc.id);
    for (const a of alunosCC) {
      console.log(`    - ${descreverAluno(a)}`);
    }

    cabecalho("10) listarCursosDepartamento(DComp)");
    const cursos = await api.listarCursosDepartamento(dComp.id);
    for (const c of cursos) {
      console.log(`    - ${c.nomeCurso}`);
    }

    cabecalho("11) emitirFolhaPagamento(Ana) -- caso de erro esperado");
    try {
      await api.emitirFolhaPagamento(ana);
      console.log("    !! deveria ter falhado");
    } catch (e) {
      console.log(`    -> erro recebido do servidor: ${e.message}`);
    }

    console.log();
    console.log("[cliente-js] Cenarios concluidos.");
  } catch (e) {
    console.error("[cliente-js] Falha:", e.message);
    process.exit(1);
  }
}

main();
