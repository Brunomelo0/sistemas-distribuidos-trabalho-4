package br.com.suauniversidade.service;

import br.com.suauniversidade.exception.RecursoNaoEncontradoException;
import br.com.suauniversidade.mensageria.PublicadorEventos;
import br.com.suauniversidade.model.Aluno;
import br.com.suauniversidade.model.Curso;
import br.com.suauniversidade.model.Departamento;
import br.com.suauniversidade.repository.RepositorioObjetos;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CursoService {

    private final RepositorioObjetos repositorio;
    private final PublicadorEventos publicador;

    public CursoService(RepositorioObjetos repositorio, PublicadorEventos publicador) {
        this.repositorio = repositorio;
        this.publicador = publicador;
    }

    public Curso criarCurso(String idDepartamento, String nomeCurso) {
        Departamento departamento = repositorio.resolverDepartamento(idDepartamento);
        if (departamento == null) {
            throw new RecursoNaoEncontradoException("Departamento nao encontrado: " + idDepartamento);
        }
        Curso curso = repositorio.registrarCurso(new Curso(nomeCurso));
        departamento.adicionarCurso(curso);
        System.out.printf("  [curso-service] Curso criado: %s (%s) no departamento %s%n",
                nomeCurso, curso.getId(), departamento.getNome());
        publicador.publicar("curso.criado", Map.of(
                "id", curso.getId(),
                "nomeCurso", curso.getNomeCurso(),
                "idDepartamento", departamento.getId(),
                "departamento", departamento.getNome()));
        return curso;
    }

    public int matricularAluno(String idCurso, Aluno aluno) {
        Curso curso = repositorio.resolverCurso(idCurso);
        if (curso == null) {
            throw new RecursoNaoEncontradoException("Curso nao encontrado: " + idCurso);
        }
        curso.matricular(aluno);
        System.out.printf("  [curso-service] %s matriculado em %s (total: %d)%n",
                aluno.getNome(), curso.getNomeCurso(), curso.totalMatriculados());
        publicador.publicar("aluno.matriculado", Map.of(
                "idCurso", curso.getId(),
                "curso", curso.getNomeCurso(),
                "alunoNome", aluno.getNome(),
                "tipo", aluno.getTipo(),
                "matricula", aluno.getMatricula(),
                "totalMatriculados", curso.totalMatriculados()));
        return curso.totalMatriculados();
    }

    public List<Aluno> listarAlunosCurso(String idCurso) {
        Curso curso = repositorio.resolverCurso(idCurso);
        if (curso == null) {
            throw new RecursoNaoEncontradoException("Curso nao encontrado: " + idCurso);
        }
        System.out.printf("  [curso-service] Listando %d alunos do curso %s%n",
                curso.totalMatriculados(), curso.getNomeCurso());
        return curso.getAlunos();
    }
}
