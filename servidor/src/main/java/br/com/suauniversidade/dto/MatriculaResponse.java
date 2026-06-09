package br.com.suauniversidade.dto;

public class MatriculaResponse {
    private boolean matriculado;
    private int totalMatriculados;

    public MatriculaResponse() {}

    public MatriculaResponse(boolean matriculado, int totalMatriculados) {
        this.matriculado = matriculado;
        this.totalMatriculados = totalMatriculados;
    }

    public boolean isMatriculado() { return matriculado; }
    public void setMatriculado(boolean matriculado) { this.matriculado = matriculado; }
    public int getTotalMatriculados() { return totalMatriculados; }
    public void setTotalMatriculados(int totalMatriculados) { this.totalMatriculados = totalMatriculados; }
}
