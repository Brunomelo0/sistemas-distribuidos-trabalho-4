export class ClienteBroker {
  constructor(urlBroker, grupo, topicos) {
    this.urlBroker = urlBroker.replace(/\/+$/, "");
    this.grupo = grupo;
    this.topicos = topicos.join(",");
  }

  async confirmar(offset) {
    await fetch(`${this.urlBroker}/confirmar`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ grupo: this.grupo, offset }),
    });
  }

  async iniciar(processar) {
    const alvo = `${this.urlBroker}/consumir?grupo=${encodeURIComponent(this.grupo)}&topicos=${encodeURIComponent(this.topicos)}&max=50`;
    while (true) {
      let resposta;
      try {
        const r = await fetch(alvo);
        resposta = await r.json();
      } catch (e) {
        console.log(`[${this.grupo}] broker indisponivel; nova tentativa em 2s...`);
        await new Promise((res) => setTimeout(res, 2000));
        continue;
      }
      for (const evento of resposta.eventos) {
        await processar(evento);
      }
      if (resposta.eventos.length > 0) {
        await this.confirmar(resposta.proximoOffset);
      }
    }
  }
}
