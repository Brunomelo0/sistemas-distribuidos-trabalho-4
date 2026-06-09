package br.com.suauniversidade.model;

public class AlunoGraduacao extends Aluno {

    public AlunoGraduacao() {
        super();
        this.tipo = "AlunoGraduacao";
    }

    public AlunoGraduacao(int id, String nome, String matricula) {
        super(id, nome, matricula);
        this.tipo = "AlunoGraduacao";
    }
}
