package br.com.suauniversidade.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

public class Curso {

    private String id;
    private String nomeCurso;

    @JsonIgnore
    private final List<Aluno> alunos = new ArrayList<>();

    public Curso() {}

    public Curso(String nomeCurso) {
        this.nomeCurso = nomeCurso;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNomeCurso() { return nomeCurso; }
    public void setNomeCurso(String nomeCurso) { this.nomeCurso = nomeCurso; }

    public List<Aluno> getAlunos() { return alunos; }

    public void matricular(Aluno aluno) {
        this.alunos.add(aluno);
    }

    public int totalMatriculados() {
        return alunos.size();
    }
}
