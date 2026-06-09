package br.com.suauniversidade.dto;

import br.com.suauniversidade.model.Departamento;

public class DepartamentoResponse {
    private String id;
    private String nome;

    public DepartamentoResponse() {}

    public DepartamentoResponse(Departamento departamento) {
        this.id = departamento.getId();
        this.nome = departamento.getNome();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}
