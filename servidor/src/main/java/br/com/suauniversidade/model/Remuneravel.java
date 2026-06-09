package br.com.suauniversidade.model;

public interface Remuneravel {
    int getDiasTrabalhados();
    void setDiasTrabalhados(int dias);
    double getValorBolsa();
    void setValorBolsa(double valor);
    double calcularPagamento();
}
