package br.com.suauniversidade.service;

import br.com.suauniversidade.exception.NaoRemuneravelException;
import br.com.suauniversidade.mensageria.PublicadorEventos;
import br.com.suauniversidade.model.Aluno;
import br.com.suauniversidade.model.Remuneravel;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AlunoService {

    private final PublicadorEventos publicador;

    public AlunoService(PublicadorEventos publicador) {
        this.publicador = publicador;
    }

    public double emitirFolhaPagamento(Aluno aluno) {
        if (!(aluno instanceof Remuneravel)) {
            throw new NaoRemuneravelException(
                    "Aluno do tipo " + aluno.getTipo() + " nao e' Remuneravel.");
        }
        Remuneravel remuneravel = (Remuneravel) aluno;
        double pagamento = remuneravel.calcularPagamento();
        System.out.printf("  [aluno-service] Folha de %s: R$ %.2f (%d dias, bolsa R$ %.2f)%n",
                aluno.getNome(), pagamento,
                remuneravel.getDiasTrabalhados(), remuneravel.getValorBolsa());
        publicador.publicar("folha.emitida", Map.of(
                "alunoNome", aluno.getNome(),
                "tipo", aluno.getTipo(),
                "pagamento", pagamento,
                "diasTrabalhados", remuneravel.getDiasTrabalhados(),
                "valorBolsa", remuneravel.getValorBolsa()));
        return pagamento;
    }
}
