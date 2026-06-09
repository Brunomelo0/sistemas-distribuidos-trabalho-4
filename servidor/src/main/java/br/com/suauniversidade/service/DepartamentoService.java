package br.com.suauniversidade.service;

import br.com.suauniversidade.exception.RecursoNaoEncontradoException;
import br.com.suauniversidade.mensageria.PublicadorEventos;
import br.com.suauniversidade.model.Curso;
import br.com.suauniversidade.model.Departamento;
import br.com.suauniversidade.repository.RepositorioObjetos;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DepartamentoService {

    private final RepositorioObjetos repositorio;
    private final PublicadorEventos publicador;

    public DepartamentoService(RepositorioObjetos repositorio, PublicadorEventos publicador) {
        this.repositorio = repositorio;
        this.publicador = publicador;
    }

    public Departamento criarDepartamento(String nome) {
        Departamento departamento = repositorio.registrarDepartamento(new Departamento(nome));
        System.out.printf("  [departamento-service] Departamento criado: %s (%s)%n",
                nome, departamento.getId());
        publicador.publicar("departamento.criado", Map.of(
                "id", departamento.getId(),
                "nome", departamento.getNome()));
        return departamento;
    }

    public List<Curso> listarCursosDepartamento(String idDepartamento) {
        Departamento departamento = repositorio.resolverDepartamento(idDepartamento);
        if (departamento == null) {
            throw new RecursoNaoEncontradoException("Departamento nao encontrado: " + idDepartamento);
        }
        System.out.printf("  [departamento-service] Departamento %s tem %d cursos%n",
                departamento.getNome(), departamento.getCursos().size());
        return departamento.getCursos();
    }
}
