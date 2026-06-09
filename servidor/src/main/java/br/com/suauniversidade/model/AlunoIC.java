package br.com.suauniversidade.model;

public class AlunoIC extends AlunoGraduacao implements Remuneravel {

    private int diasTrabalhados;
    private double valorBolsa;

    public AlunoIC() {
        super();
        this.tipo = "AlunoIC";
    }

    public AlunoIC(int id, String nome, String matricula) {
        super(id, nome, matricula);
        this.tipo = "AlunoIC";
    }

    @Override public int getDiasTrabalhados() { return diasTrabalhados; }
    @Override public void setDiasTrabalhados(int dias) { this.diasTrabalhados = dias; }
    @Override public double getValorBolsa() { return valorBolsa; }
    @Override public void setValorBolsa(double valor) { this.valorBolsa = valor; }

    @Override
    public double calcularPagamento() {
        return diasTrabalhados * (valorBolsa / 30.0);
    }
}
