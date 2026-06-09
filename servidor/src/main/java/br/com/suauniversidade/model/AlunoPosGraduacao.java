package br.com.suauniversidade.model;

public class AlunoPosGraduacao extends Aluno implements Remuneravel {

    private int diasTrabalhados;
    private double valorBolsa;

    public AlunoPosGraduacao() {
        super();
        this.tipo = "AlunoPosGraduacao";
    }

    public AlunoPosGraduacao(int id, String nome, String matricula) {
        super(id, nome, matricula);
        this.tipo = "AlunoPosGraduacao";
    }

    @Override public int getDiasTrabalhados() { return diasTrabalhados; }
    @Override public void setDiasTrabalhados(int dias) { this.diasTrabalhados = dias; }
    @Override public double getValorBolsa() { return valorBolsa; }
    @Override public void setValorBolsa(double valor) { this.valorBolsa = valor; }

    @Override
    public double calcularPagamento() {
        return diasTrabalhados * (valorBolsa / 30.0) * 1.10;
    }
}
