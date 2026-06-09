# Relatório Técnico — Trabalho 4: Comunicação Indireta

**Disciplina:** Sistemas Distribuídos (QXD0043) — UFC Quixadá
**Projeto base:** Trabalho 3 — Web Service / API REST (controle de alunos, cursos e departamentos)
**Abordagem escolhida:** Opção B — Sistemas Publicar-Assinar (Publish-Subscribe)

---

## 1. Ponto de partida e problema de acoplamento

No Trabalho 3 toda a comunicação é **direta e síncrona**: o cliente faz uma requisição HTTP
(`POST /cursos/{id}/alunos`) e o `CursoService` resolve a operação no mesmo instante e devolve a
resposta. Esse modelo é eficiente, mas impõe acoplamento ponto a ponto em duas dimensões:

- **Acoplamento espacial:** quem quiser reagir a um fato do domínio (uma matrícula, uma folha
  emitida) precisaria ser chamado explicitamente pelo serviço, que teria de conhecer o endereço de
  cada interessado.
- **Acoplamento temporal:** emissor e receptor têm de estar no ar ao mesmo tempo. Se um componente
  que reage à matrícula estiver fora, ou a operação falha, ou o evento se perde.

Esse é exatamente o tipo de rigidez que o Capítulo 6 ataca. O sistema tem **fatos de domínio
naturais** (`departamento.criado`, `curso.criado`, `aluno.matriculado`, `folha.emitida`) que
interessam a múltiplos componentes independentes — um candidato ideal para indireção.

## 2. Justificativa da escolha (Publish-Subscribe)

Entre as quatro abordagens, **Publicar-Assinar** é a mais aderente ao projeto porque os eventos do
domínio têm **vários interessados distintos e independentes**, e cada um quer reagir à sua maneira:
um serviço de **notificação** (avisar o aluno) e um serviço de **auditoria** (registrar tudo). Com um
intermediário (broker) e a abstração de **tópicos**, o servidor apenas *anuncia o que aconteceu* sem
saber quem ouve; novos assinantes entram depois sem tocar no produtor. Isso entrega o
**desacoplamento espacial e temporal completo** que a Opção B exige.

Comunicação em Grupo (A) resolveria a disseminação, mas pressupõe gestão de membros/visões e foca
em ordenação entre réplicas — desnecessário aqui. Filas (C) atenderiam um consumidor por mensagem,
mas perderiam a riqueza do *fan-out* para assinantes heterogêneos. Espaço de Tuplas (D) é um modelo
de memória associativa que não casa com "reagir a fatos". Pub-Sub é o ajuste certo.

**Decisão de implementação — broker próprio, sem infraestrutura externa.** Em vez de adotar
RabbitMQ/Kafka, implementei o intermediário do zero: um **broker em Node.js puro** (módulo `http`,
**sem nenhuma dependência**). Foram dois motivos. Primeiro, o requisito de não depender de
infraestrutura externa (nada de Docker ou serviço a instalar): tudo sobe com `mvn spring-boot:run` e
`node`. Segundo, e mais importante para a disciplina, **construir o intermediário evidencia o domínio
dos conceitos** — log de eventos, roteamento por tópico, offsets por assinante e persistência ficam
explícitos, em vez de escondidos atrás de uma caixa-preta.

## 3. Arquitetura da solução

```
  Produtor (servidor REST, Java/Spring)         BROKER pub-sub (Node)            Assinantes (Node)
  ───────────────────────────────────           ────────────────────            ─────────────────
  CursoService / AlunoService /                  POST /publicar  ─┐              servico-notificacao
  DepartamentoService                                             │              (aluno.matriculado,
        │ publicar(topico, dados)                 log append-only │               folha.emitida)
        ▼                                          em disco        │
  PublicadorEventos ──HTTP──►  /publicar  ──►  [ seq | topico | dados | ts ]  ◄──GET /consumir (long-poll)
        ▲ outbox (reenfileira                     offsets por grupo            servico-auditoria  ("#")
          se o broker cair)                       em disco           ──►       POST /confirmar (offset)
```

- **Tópicos:** `departamento.criado`, `curso.criado`, `aluno.matriculado`, `folha.emitida`.
- **Produtor:** o componente `PublicadorEventos` no servidor publica via `HttpClient` do JDK (sem
  dependência Maven nova). A publicação é **assíncrona, fire-and-forget** — não bloqueia a resposta
  HTTP nem quebra a API se o broker estiver fora.
- **Broker:** mantém um **log ordenado e durável** (arquivo `log.ndjson`, uma linha por evento) e um
  **offset confirmado por grupo de assinante** (`offsets.json`). Entrega por *pull* com **long-poll**
  (até 25 s aguardando novidade), evitando *busy-wait*. O modelo de offset por grupo dá *fan-out*:
  cada grupo recebe sua própria cópia, no seu próprio ritmo.
- **Assinantes:** processos independentes que declaram os tópicos de interesse, processam e confirmam
  o offset. `notificacao` assina dois tópicos; `auditoria` assina todos (`#`) e grava em disco.

Note a **heterogeneidade**: produtor em Java, broker e assinantes em Node, conversando só pela
abstração de tópicos sobre JSON — outra evidência do desacoplamento.

## 4. Desacoplamento demonstrado

- **Espacial:** o servidor só conhece a URL do broker (`broker.url`). Ele **nunca** configura IP/porta
  de notificação ou auditoria — não sabe sequer que existem. Assinantes podem ser adicionados sem
  alterar o produtor.
- **Temporal:** com o assinante de notificação **desligado**, o servidor publicou cinco eventos com
  sucesso (respondeu normalmente aos clientes). Ao subir a notificação **depois**, ela leu o log a
  partir do seu offset e **processou todo o atraso**, recebendo apenas os tópicos que assina e
  ignorando os demais. Emissor e receptor têm tempos de vida independentes.

## 5. Robustez e tratamento de falhas

O sistema foi projetado para os três modos de falha relevantes:

1. **Assinante cai:** os eventos ficam **retidos no log durável** do broker; ao retornar, o assinante
   retoma do último offset confirmado. Entrega **pelo-menos-uma-vez** — se ele cair *depois* de
   processar mas *antes* de confirmar, reprocessa na volta (sem perda).
2. **Broker cai:** `log.ndjson` e `offsets.json` sobrevivem em disco. Ao religar, o broker **recupera
   o estado** (mesmo `seq` e mesmos offsets) e a operação continua de onde parou — testado e
   confirmado.
3. **Broker indisponível no momento da publicação:** o produtor não perde o evento nem falha a
   requisição do cliente. O `PublicadorEventos` guarda o evento numa **outbox** em memória e um
   *flusher* agendado **reenfileira e reenvia** quando o broker volta, preservando a ordem.

## 6. Análise: overhead, complexidade e mitigação

A indireção tem custo. **Latência:** um evento agora passa por um salto extra (servidor → broker →
assinante) em vez de uma chamada direta; o produtor também grava em disco a cada publicação. **CPU/IO:**
o broker faz *append* síncrono no log a cada evento. **Complexidade operacional:** há mais processos
para subir e monitorar, e a entrega pelo-menos-uma-vez transfere ao consumidor a responsabilidade de
lidar com duplicatas.

Como isso afetou o sistema e como mitigamos:

- A latência extra **não impacta o caminho crítico do cliente**, porque a publicação é assíncrona e
  desacoplada da resposta HTTP — o usuário não espera o broker. O custo recai sobre um fluxo de fundo.
- O **long-poll** elimina o desperdício de *polling* em vazio: o assinante só é acordado quando há
  evento, mantendo o consumo de CPU baixo quando o sistema está ocioso.
- O custo de IO do log é mitigável com *batching* de gravação (acumular N eventos por *flush*) ou
  rotação/compactação do log; para a escala do projeto, o *append* por evento é suficiente.
- Para as duplicatas do pelo-menos-uma-vez, a mitigação é **idempotência no consumidor** (deduplicar
  por `seq`), que o modelo de offset já facilita.

O balanço é favorável: trocou-se um pouco de desempenho de fundo e um pouco mais de processos por
**flexibilidade, resiliência e extensibilidade** — exatamente o que a comunicação indireta promete.

## 7. Conclusão

A evolução substituiu o acoplamento ponto a ponto por um intermediário pub-sub com tópicos, log
durável e offsets por assinante. O sistema prova desacoplamento espacial (o produtor ignora os
receptores) e temporal (publica com o receptor offline e este recupera o atraso), e é resiliente à
queda de assinante, de broker e do próprio broker durante a publicação — sem nenhuma dependência ou
infraestrutura externa.
