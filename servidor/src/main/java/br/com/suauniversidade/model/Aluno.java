package br.com.suauniversidade.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "tipo",
        visible = true,
        defaultImpl = Aluno.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AlunoGraduacao.class, name = "AlunoGraduacao"),
        @JsonSubTypes.Type(value = AlunoIC.class, name = "AlunoIC"),
        @JsonSubTypes.Type(value = AlunoPosGraduacao.class, name = "AlunoPosGraduacao")
})
public class Aluno {

    protected String tipo;
    protected int id;
    protected String nome;
    protected String matricula;

    public Aluno() {
        this.tipo = this.getClass().getSimpleName();
    }

    public Aluno(int id, String nome, String matricula) {
        this();
        this.id = id;
        this.nome = nome;
        this.matricula = matricula;
    }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    @Override
    public String toString() {
        return String.format("%s[id=%d, nome=%s, matricula=%s]", tipo, id, nome, matricula);
    }
}
