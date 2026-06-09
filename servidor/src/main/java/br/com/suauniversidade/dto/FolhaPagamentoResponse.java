package br.com.suauniversidade.dto;

public class FolhaPagamentoResponse {
    private String nome;
    private double pagamento;

    public FolhaPagamentoResponse() {}

    public FolhaPagamentoResponse(String nome, double pagamento) {
        this.nome = nome;
        this.pagamento = pagamento;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public double getPagamento() { return pagamento; }
    public void setPagamento(double pagamento) { this.pagamento = pagamento; }
}
