# Trabalho 4 — Comunicação Indireta (QXD0043 — Sistemas Distribuídos)

Evolução da arquitetura do **Trabalho 3 (API REST)** com um nível de indireção na
comunicação, usando **Publicar-Assinar (Publish-Subscribe)**. O relatório técnico está
em [`RELATORIO-T4.md`](RELATORIO-T4.md).

A camada REST do T3 continua intacta e documentada mais abaixo nesta página. O T4
**acrescenta** um broker pub-sub e dois serviços assinantes, **sem nenhuma dependência
nova nem infraestrutura externa** (nada de Docker): tudo sobe com `mvn spring-boot:run` e
`node`.

## Arquitetura do T4

```
  Produtor (servidor REST, Java)        BROKER pub-sub (Node, sem deps)        Assinantes (Node)
  ──────────────────────────────        ──────────────────────────────        ─────────────────
  CursoService / AlunoService /         POST /publicar                         servico-notificacao
  DepartamentoService                   log append-only em disco               (aluno.matriculado,
        │ publicar(topico, dados)       offsets por grupo em disco              folha.emitida)
        ▼                               GET  /consumir (long-poll)             servico-auditoria
  PublicadorEventos ──HTTP──►           POST /confirmar (offset)               (assina tudo: "#")
        ▲ outbox reenfileira
          se o broker cair
```

- **Tópicos:** `departamento.criado`, `curso.criado`, `aluno.matriculado`, `folha.emitida`.
- **Produtor:** o componente `PublicadorEventos` no servidor publica de forma **assíncrona**
  (via `HttpClient` do JDK). Se o broker estiver fora, o evento espera numa **outbox** e é
  reenviado depois — a API REST nunca quebra nem bloqueia por causa do broker.
- **Broker** (`broker/broker.js`): mantém um **log durável** (`log.ndjson`) e um **offset por
  grupo de assinante** (`offsets.json`). Entrega por *pull* com long-poll.
- **Assinantes** (`assinantes/`): processos independentes; cada grupo recebe sua própria cópia
  dos eventos (fan-out) e confirma seu offset.

## Como executar o T4 (4 terminais)

```bash
# Terminal 1 — broker pub-sub (Node 18+, sem instalar nada)
cd broker
node broker.js                 # sobe em http://localhost:9000

# Terminal 2 — servidor REST (produtor de eventos) — JDK 17+ e Maven
cd servidor
mvn spring-boot:run            # API em http://localhost:8080, publica no broker

# Terminal 3 — assinante de notificação
cd assinantes
node servico-notificacao.js

# Terminal 4 — assinante de auditoria
cd assinantes
node servico-auditoria.js
```

Depois rode qualquer cliente do T3 (`cliente-js` ou `cliente-python`) para gerar os fatos de
domínio. Os eventos aparecem nos dois assinantes. `GET http://localhost:9000/status` mostra o
estado do broker (eventos por tópico e offsets).

## Roteiro de demonstração (desacoplamento em tempo real)

1. **Desacoplamento espacial:** suba o broker, o servidor e os clientes. O servidor não tem
   nenhum endereço de assinante configurado — só a URL do broker em `application.properties`.
2. **Desacoplamento temporal:** **não** suba o `servico-notificacao` ainda. Rode o cliente
   (matrículas e folhas). O servidor responde normalmente e os eventos ficam **retidos** no
   broker. Agora suba o `servico-notificacao`: ele **recupera todo o atraso** e processa os
   eventos que perdeu enquanto estava offline.
3. **Queda de nó / recuperação de estado:** derrube o broker (Ctrl+C) e religue. O log e os
   offsets persistem em disco; ele **recupera o estado** e a operação continua de onde parou.
4. **Resiliência do produtor:** derrube o broker e rode o cliente. A API REST continua
   respondendo; os eventos esperam na outbox do servidor e são publicados quando o broker volta.

---

# Trabalho 3 — Web Services / API (camada REST, base do T4)

Reimplementação do serviço remoto do **Trabalho 2 (RMI)** usando os conceitos de
**API REST**, organizada no protocolo **requisição/resposta** sobre HTTP.
**Não há sockets nem RMI** neste trabalho.

O domínio é o mesmo do T2: **controle de alunos, cursos e departamentos** de uma
universidade.

---

## 1. Arquitetura

```
┌─────────────────────────┐         HTTP (JSON)         ┌──────────────────────────────────────┐
│   Clientes (2 línguas)  │  requisição  ───────────►   │      Servidor — API REST (Java)        │
│                         │                             │           Spring Boot                  │
│  cliente-js  (Node)     │                             │  ┌──────────────────────────────────┐  │
│   └ ApiServico          │                             │  │ DepartamentoController  (objeto 1) │  │
│  cliente-python         │  ◄───────────  resposta     │  │ CursoController         (objeto 2) │  │
│   └ ApiServico          │                             │  │ AlunoController         (objeto 3) │  │
│                         │                             │  └───────────────┬──────────────────┘  │
└─────────────────────────┘                             │   Services ──►  RepositorioObjetos     │
                                                         │              (em memória, por ref.)    │
                                                         └────────────────────────────────────────┘
```

- **Protocolo requisição/resposta:** o próprio HTTP (método + caminho + corpo → status + corpo).
- **Representação externa de dados:** JSON (serialização via Jackson no servidor).
- **Servidor e clientes são projetos separados** (`servidor/`, `cliente-js/`, `cliente-python/`).
- **Linguagem do serviço:** Java. **Clientes:** JavaScript (Node) e Python — ambas diferentes da do serviço.

---

## 2. Os três objetos distribuídos (lado servidor)

O enunciado exige pelo menos 3 objetos distribuídos. São os três controllers REST,
cada um com seu serviço de domínio:

| # | Objeto distribuído        | Recurso REST            | Operações remotas que expõe                          |
|---|---------------------------|-------------------------|------------------------------------------------------|
| 1 | `DepartamentoController`  | `/departamentos`        | `criarDepartamento`, `listarCursosDepartamento`      |
| 2 | `CursoController`         | `/cursos`               | `criarCurso`, `matricularAluno`, `listarAlunosCurso` |
| 3 | `AlunoController`         | `/alunos`               | `emitirFolhaPagamento`                               |

São **6 operações remotas** (mínimo exigido: 4).

---

## 3. Equivalência Trabalho 2 (RMI) → Trabalho 3 (REST)

| Conceito do T2 (RMI / protocolo R-R)                         | Equivalente no T3 (REST)                                  |
|--------------------------------------------------------------|-----------------------------------------------------------|
| `ProxyServico.doOperation(...)`                              | classe `ApiServico` no cliente (faz a requisição HTTP)    |
| `DespachanteServico.getRequest()` / `sendReply()`            | roteamento do Spring + serialização da resposta           |
| `Mensagem` (messageType, requestId, objectReference, ...)    | requisição/resposta HTTP (método, caminho, corpo, status) |
| `Marshaller` (JSON via Gson)                                 | serialização JSON via Jackson                             |
| `RemoteObjectRef` (referência a objeto remoto)               | **id do recurso na URI** (`departamento-1`, `curso-2`)    |
| `RepositorioObjetosRemotos` (ConcurrentHashMap no servidor)  | `RepositorioObjetos` (ConcurrentHashMap em memória)       |
| desserialização polimórfica via `AlunoDeserializer` (Gson)   | `@JsonTypeInfo` + `@JsonSubTypes` (Jackson)               |

### Passagem por referência (objetos remotos)
`Departamento` e `Curso` são criados e **vivem no servidor**, dentro do
`RepositorioObjetos`. O cliente guarda apenas o **id** (a referência). Toda operação
que precise deles envia esse id **na URI** (ex.: `POST /cursos/curso-2/alunos`), e o
servidor resolve o objeto no repositório.

### Passagem por valor (objetos locais do cliente)
`Aluno` (e suas subclasses) é criado no cliente, **empacotado em JSON** e reconstruído
no servidor. Como o tipo é polimórfico, o campo `tipo` (discriminador) instrui o Jackson
a reconstruir a subclasse correta (`AlunoGraduacao`, `AlunoIC`, `AlunoPosGraduacao`).

---

## 4. Modelo de entidades

| Entidade              | Tipo                                            | Relacionamento                |
|-----------------------|-------------------------------------------------|-------------------------------|
| `Aluno`               | classe base                                     | —                             |
| `AlunoGraduacao`      | extends `Aluno`                                 | **é-um** Aluno                |
| `AlunoIC`             | extends `AlunoGraduacao` + implements `Remuneravel` | **é-um** AlunoGraduacao   |
| `AlunoPosGraduacao`   | extends `Aluno` + implements `Remuneravel`      | **é-um** Aluno                |
| `Curso`               | entidade                                        | **tem-uma** lista de `Aluno`  |
| `Departamento`        | entidade                                        | **tem-uma** lista de `Curso`  |
| `Remuneravel`         | interface                                       | contrato de pagamento         |

**6 entidades**, **3 heranças** (é-um) e **2 agregações** (tem-um) — supera os mínimos (≥4 / ≥2 / ≥2).

Regras de pagamento:
- `AlunoIC`: `diasTrabalhados × (valorBolsa / 30)`
- `AlunoPosGraduacao`: `diasTrabalhados × (valorBolsa / 30) × 1.10`
- `emitirFolhaPagamento` em um aluno **não** `Remuneravel` retorna erro (HTTP 422).

---

## 5. Endpoints (as 6 operações remotas)

| # | Operação                     | Método e caminho                               | Corpo (requisição)                         | Resposta                                  |
|---|------------------------------|------------------------------------------------|--------------------------------------------|-------------------------------------------|
| 1 | `criarDepartamento`          | `POST /departamentos`                          | `{ "nome": "..." }`                        | `201` `{ "id", "nome" }`                  |
| 2 | `criarCurso`                 | `POST /departamentos/{idDep}/cursos`           | `{ "nomeCurso": "..." }`                   | `201` `{ "id", "nomeCurso" }`             |
| 3 | `matricularAluno`            | `POST /cursos/{idCurso}/alunos`                | objeto `Aluno` (por valor)                 | `201` `{ "matriculado", "totalMatriculados" }` |
| 4 | `listarAlunosCurso`          | `GET  /cursos/{idCurso}/alunos`                | —                                          | `200` `[ Aluno, ... ]`                    |
| 5 | `listarCursosDepartamento`   | `GET  /departamentos/{idDep}/cursos`           | —                                          | `200` `[ { "id", "nomeCurso" }, ... ]`    |
| 6 | `emitirFolhaPagamento`       | `POST /alunos/folha-pagamento`                 | objeto `Aluno` (por valor)                 | `200` `{ "nome", "pagamento" }`           |

Exemplo de objeto `Aluno` (por valor) enviado pelo cliente:

```json
{ "tipo": "AlunoIC", "id": 2, "nome": "Bruno Costa", "matricula": "2024002", "diasTrabalhados": 20, "valorBolsa": 700.0 }
```

### Códigos de status e erros
- `404 Not Found` — departamento ou curso inexistente (referência inválida).
- `422 Unprocessable Entity` — aluno não `Remuneravel` na folha de pagamento.
- `400 Bad Request` — demais erros.

Corpo de erro padrão: `{ "sucesso": false, "erro": "mensagem" }`.

---

## 6. Como executar

Suba o servidor em um terminal e cada cliente em outro.

### Servidor (Java + Spring Boot) — requer JDK 17+ e Maven

```bash
cd servidor
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`.

### Cliente JavaScript (Node) — requer Node 18+

```bash
cd cliente-js
node cliente.js
# opcional, outro endereço: node cliente.js http://localhost:8080
```

### Cliente Python — requer Python 3.9+

```bash
cd cliente-python
pip install -r requirements.txt
python cliente.py
# opcional, outro endereço: python cliente.py http://localhost:8080
```

---

## 7. Saída esperada (qualquer um dos clientes)

```
=== 7) emitirFolhaPagamento(Bruno) ===
    -> retorno: R$ 466.67
=== 8) emitirFolhaPagamento(Carla) ===
    -> retorno: R$ 2420.00
=== 9) listarAlunosCurso(CC) ===
    - AlunoGraduacao[id=1, nome=Ana Silva, matricula=2024001]
    - AlunoIC[id=2, nome=Bruno Costa, matricula=2024002]
=== 11) emitirFolhaPagamento(Ana) -- caso de erro esperado ===
    -> erro recebido do servidor: Aluno do tipo AlunoGraduacao nao e' Remuneravel.
```

No console do servidor aparecem os logs de cada objeto distribuído
(`[departamento-service]`, `[curso-service]`, `[aluno-service]`).

---

## 8. Atendimento aos requisitos do enunciado

| Requisito                                                              | Onde                                                            |
|------------------------------------------------------------------------|-----------------------------------------------------------------|
| Reimplementar o serviço do T2 com WS/API (sem sockets nem RMI)         | API REST com Spring Boot                                        |
| Protocolo requisição/resposta                                          | HTTP (método + caminho + corpo → status + corpo)                |
| ≥ 3 objetos distribuídos no servidor                                   | `DepartamentoController`, `CursoController`, `AlunoController`   |
| Servidor em projeto separado do cliente                                | `servidor/` separado de `cliente-js/` e `cliente-python/`       |
| Cliente em ≥ 2 línguas diferentes da do serviço                        | JavaScript (Node) e Python (serviço em Java)                    |
| ≥ 4 entidades, ≥ 2 heranças, ≥ 2 agregações                            | 6 entidades, 3 heranças, 2 agregações                           |
| ≥ 4 métodos para invocação remota                                      | 6 operações remotas                                             |
| Passagem por referência (objetos remotos)                              | id do recurso na URI + `RepositorioObjetos`                     |
| Passagem por valor (objetos locais)                                    | `Aluno` no corpo JSON, reconstruído por Jackson polimórfico     |
| Representação externa de dados                                         | JSON (Jackson no servidor, JSON nativo nos clientes)            |

---

## 9. Estrutura do projeto

```
trabalho-3-ws-api/
├── servidor/                          (projeto Java — API REST, Spring Boot)
│   ├── pom.xml
│   └── src/main/
│       ├── java/br/com/suauniversidade/
│       │   ├── ServidorApplication.java
│       │   ├── model/                 Aluno, AlunoGraduacao, AlunoIC, AlunoPosGraduacao,
│       │   │                          Curso, Departamento, Remuneravel
│       │   ├── repository/            RepositorioObjetos
│       │   ├── service/               DepartamentoService, CursoService, AlunoService
│       │   ├── controller/            DepartamentoController, CursoController, AlunoController
│       │   ├── dto/                   requisições e respostas
│       │   └── exception/             tratamento global de erros
│       └── resources/application.properties
├── cliente-js/                        (projeto Node)
│   ├── package.json
│   ├── apiServico.js                  proxy tipado sobre HTTP
│   └── cliente.js                     cenários de teste
└── cliente-python/                    (projeto Python)
    ├── requirements.txt
    ├── api_servico.py                 proxy tipado sobre HTTP
    └── cliente.py                     cenários de teste
```

---

## 10. Vídeo e entrega

A entrega no Moodle deve conter o link do repositório e um vídeo apresentando:
o código-fonte do serviço (os 3 objetos distribuídos e o modelo de domínio) e dos
dois clientes, mais a execução com o servidor de pé e os dois clientes rodando os
cenários. Este README serve como relatório dos serviços remotos implementados.
