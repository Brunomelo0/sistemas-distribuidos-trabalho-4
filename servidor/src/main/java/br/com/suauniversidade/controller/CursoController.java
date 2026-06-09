package br.com.suauniversidade.controller;

import br.com.suauniversidade.dto.CriarCursoRequest;
import br.com.suauniversidade.dto.CursoResponse;
import br.com.suauniversidade.dto.MatriculaResponse;
import br.com.suauniversidade.model.Aluno;
import br.com.suauniversidade.model.Curso;
import br.com.suauniversidade.service.CursoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @PostMapping("/departamentos/{idDepartamento}/cursos")
    public ResponseEntity<CursoResponse> criarCurso(@PathVariable String idDepartamento,
                                                    @RequestBody CriarCursoRequest requisicao) {
        Curso curso = cursoService.criarCurso(idDepartamento, requisicao.getNomeCurso());
        return ResponseEntity.status(HttpStatus.CREATED).body(new CursoResponse(curso));
    }

    @PostMapping("/cursos/{idCurso}/alunos")
    public ResponseEntity<MatriculaResponse> matricularAluno(@PathVariable String idCurso,
                                                             @RequestBody Aluno aluno) {
        int total = cursoService.matricularAluno(idCurso, aluno);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MatriculaResponse(true, total));
    }

    @GetMapping("/cursos/{idCurso}/alunos")
    public ResponseEntity<List<Aluno>> listarAlunosCurso(@PathVariable String idCurso) {
        return ResponseEntity.ok(cursoService.listarAlunosCurso(idCurso));
    }
}
