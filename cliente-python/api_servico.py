import requests


class ApiServico:
    def __init__(self, base_url="http://localhost:8080"):
        self.base_url = base_url.rstrip("/")

    def _requisitar(self, metodo, caminho, corpo=None):
        url = self.base_url + caminho
        resposta = requests.request(
            metodo, url, json=corpo,
            headers={"Content-Type": "application/json"},
        )
        dados = resposta.json() if resposta.text else None
        if not resposta.ok:
            mensagem = dados.get("erro") if isinstance(dados, dict) and dados.get("erro") else f"HTTP {resposta.status_code}"
            raise RuntimeError(mensagem)
        return dados

    def criar_departamento(self, nome):
        return self._requisitar("POST", "/departamentos", {"nome": nome})

    def criar_curso(self, id_departamento, nome_curso):
        return self._requisitar("POST", f"/departamentos/{id_departamento}/cursos", {"nomeCurso": nome_curso})

    def matricular_aluno(self, id_curso, aluno):
        return self._requisitar("POST", f"/cursos/{id_curso}/alunos", aluno)

    def emitir_folha_pagamento(self, aluno):
        return self._requisitar("POST", "/alunos/folha-pagamento", aluno)

    def listar_alunos_curso(self, id_curso):
        return self._requisitar("GET", f"/cursos/{id_curso}/alunos")

    def listar_cursos_departamento(self, id_departamento):
        return self._requisitar("GET", f"/departamentos/{id_departamento}/cursos")
