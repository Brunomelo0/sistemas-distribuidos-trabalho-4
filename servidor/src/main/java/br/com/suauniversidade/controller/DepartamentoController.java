package br.com.suauniversidade.controller;

import br.com.suauniversidade.dto.CriarDepartamentoRequest;
import br.com.suauniversidade.dto.CursoResponse;
import br.com.suauniversidade.dto.DepartamentoResponse;
import br.com.suauniversidade.model.Departamento;
import br.com.suauniversidade.service.DepartamentoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/departamentos")
public class DepartamentoController {

    private final DepartamentoService departamentoService;

    public DepartamentoController(DepartamentoService departamentoService) {
        this.departamentoService = departamentoService;
    }

    @PostMapping
    public ResponseEntity<DepartamentoResponse> criarDepartamento(@RequestBody CriarDepartamentoRequest requisicao) {
        Departamento departamento = departamentoService.criarDepartamento(requisicao.getNome());
        return ResponseEntity.status(HttpStatus.CREATED).body(new DepartamentoResponse(departamento));
    }

    @GetMapping("/{id}/cursos")
    public ResponseEntity<List<CursoResponse>> listarCursosDepartamento(@PathVariable String id) {
        List<CursoResponse> cursos = departamentoService.listarCursosDepartamento(id)
                .stream().map(CursoResponse::new).toList();
        return ResponseEntity.ok(cursos);
    }
}
