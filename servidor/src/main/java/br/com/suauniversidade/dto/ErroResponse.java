package br.com.suauniversidade.dto;

public class ErroResponse {
    private boolean sucesso = false;
    private String erro;

    public ErroResponse() {}

    public ErroResponse(String erro) {
        this.erro = erro;
    }

    public boolean isSucesso() { return sucesso; }
    public void setSucesso(boolean sucesso) { this.sucesso = sucesso; }
    public String getErro() { return erro; }
    public void setErro(String erro) { this.erro = erro; }
}
