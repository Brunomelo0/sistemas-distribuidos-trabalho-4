package br.com.suauniversidade.controller;

import br.com.suauniversidade.dto.FolhaPagamentoResponse;
import br.com.suauniversidade.model.Aluno;
import br.com.suauniversidade.service.AlunoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/alunos")
public class AlunoController {

    private final AlunoService alunoService;

    public AlunoController(AlunoService alunoService) {
        this.alunoService = alunoService;
    }

    @PostMapping("/folha-pagamento")
    public ResponseEntity<FolhaPagamentoResponse> emitirFolhaPagamento(@RequestBody Aluno aluno) {
        double pagamento = alunoService.emitirFolhaPagamento(aluno);
        return ResponseEntity.ok(new FolhaPagamentoResponse(aluno.getNome(), pagamento));
    }
}
