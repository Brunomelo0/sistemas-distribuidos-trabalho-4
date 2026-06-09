package br.com.suauniversidade.dto;

import br.com.suauniversidade.model.Curso;

public class CursoResponse {
    private String id;
    private String nomeCurso;

    public CursoResponse() {}

    public CursoResponse(Curso curso) {
        this.id = curso.getId();
        this.nomeCurso = curso.getNomeCurso();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNomeCurso() { return nomeCurso; }
    public void setNomeCurso(String nomeCurso) { this.nomeCurso = nomeCurso; }
}
