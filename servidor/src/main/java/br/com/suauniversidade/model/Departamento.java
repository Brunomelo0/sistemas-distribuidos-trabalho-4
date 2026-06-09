package br.com.suauniversidade.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

public class Departamento {

    private String id;
    private String nome;

    @JsonIgnore
    private final List<Curso> cursos = new ArrayList<>();

    public Departamento() {}

    public Departamento(String nome) {
        this.nome = nome;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public List<Curso> getCursos() { return cursos; }

    public void adicionarCurso(Curso curso) {
        this.cursos.add(curso);
    }
}
