import os
import sys

from api_servico import ApiServico


def cabecalho(titulo):
    print()
    print(f"=== {titulo} ===")


def descrever_aluno(a):
    return f"{a['tipo']}[id={a['id']}, nome={a['nome']}, matricula={a['matricula']}]"


def main():
    base_url = sys.argv[1] if len(sys.argv) > 1 else os.environ.get("SERVIDOR_URL", "http://localhost:8080")
    api = ApiServico(base_url)
    print(f"[cliente-python] conectado em {base_url}")

    try:
        cabecalho('1) criar_departamento("DEMA - Matematica")')
        d_ema = api.criar_departamento("DEMA - Matematica")
        print(f"    -> retorno: {d_ema['id']} ({d_ema['nome']})")

        cabecalho('2) criar_curso("Engenharia de Software", DEMA)')
        es = api.criar_curso(d_ema["id"], "Engenharia de Software")
        print(f"    -> retorno: {es['id']} ({es['nomeCurso']})")

        cabecalho('3) criar_curso("Redes de Computadores", DEMA)')
        rc = api.criar_curso(d_ema["id"], "Redes de Computadores")
        print(f"    -> retorno: {rc['id']} ({rc['nomeCurso']})")

        cabecalho("4) matricular_aluno(AlunoGraduacao Diego, ES)")
        diego = {"tipo": "AlunoGraduacao", "id": 10, "nome": "Diego Souza", "matricula": "2025010"}
        print(f"    aluno (cliente): {descrever_aluno(diego)}")
        api.matricular_aluno(es["id"], diego)

        cabecalho("5) matricular_aluno(AlunoIC Elena, ES)")
        elena = {"tipo": "AlunoIC", "id": 11, "nome": "Elena Rocha", "matricula": "2025011", "diasTrabalhados": 25, "valorBolsa": 800.0}
        print(f"    aluna (cliente): {descrever_aluno(elena)}")
        api.matricular_aluno(es["id"], elena)

        cabecalho("6) matricular_aluno(AlunoPosGraduacao Felipe, RC)")
        felipe = {"tipo": "AlunoPosGraduacao", "id": 12, "nome": "Felipe Alves", "matricula": "2025012", "diasTrabalhados": 28, "valorBolsa": 2500.0}
        print(f"    aluno (cliente): {descrever_aluno(felipe)}")
        api.matricular_aluno(rc["id"], felipe)

        cabecalho("7) emitir_folha_pagamento(Elena)")
        folha_elena = api.emitir_folha_pagamento(elena)
        print(f"    -> retorno: R$ {folha_elena['pagamento']:.2f}")

        cabecalho("8) emitir_folha_pagamento(Felipe)")
        folha_felipe = api.emitir_folha_pagamento(felipe)
        print(f"    -> retorno: R$ {folha_felipe['pagamento']:.2f}")

        cabecalho("9) listar_alunos_curso(ES)")
        for a in api.listar_alunos_curso(es["id"]):
            print(f"    - {descrever_aluno(a)}")

        cabecalho("10) listar_cursos_departamento(DEMA)")
        for c in api.listar_cursos_departamento(d_ema["id"]):
            print(f"    - {c['nomeCurso']}")

        cabecalho("11) emitir_folha_pagamento(Diego) -- caso de erro esperado")
        try:
            api.emitir_folha_pagamento(diego)
            print("    !! deveria ter falhado")
        except RuntimeError as e:
            print(f"    -> erro recebido do servidor: {e}")

        print()
        print("[cliente-python] Cenarios concluidos.")
    except Exception as e:
        print(f"[cliente-python] Falha: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
