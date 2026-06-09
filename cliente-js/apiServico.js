export class ApiServico {
  constructor(baseUrl = "http://localhost:8080") {
    this.baseUrl = baseUrl.replace(/\/+$/, "");
  }

  async #requisitar(metodo, caminho, corpo) {
    const opcoes = { method: metodo, headers: { "Content-Type": "application/json" } };
    if (corpo !== undefined) {
      opcoes.body = JSON.stringify(corpo);
    }
    const resposta = await fetch(this.baseUrl + caminho, opcoes);
    const texto = await resposta.text();
    const dados = texto ? JSON.parse(texto) : null;
    if (!resposta.ok) {
      const mensagem = dados && dados.erro ? dados.erro : `HTTP ${resposta.status}`;
      throw new Error(mensagem);
    }
    return dados;
  }

  criarDepartamento(nome) {
    return this.#requisitar("POST", "/departamentos", { nome });
  }

  criarCurso(idDepartamento, nomeCurso) {
    return this.#requisitar("POST", `/departamentos/${idDepartamento}/cursos`, { nomeCurso });
  }

  matricularAluno(idCurso, aluno) {
    return this.#requisitar("POST", `/cursos/${idCurso}/alunos`, aluno);
  }

  emitirFolhaPagamento(aluno) {
    return this.#requisitar("POST", "/alunos/folha-pagamento", aluno);
  }

  listarAlunosCurso(idCurso) {
    return this.#requisitar("GET", `/cursos/${idCurso}/alunos`);
  }

  listarCursosDepartamento(idDepartamento) {
    return this.#requisitar("GET", `/departamentos/${idDepartamento}/cursos`);
  }
}
