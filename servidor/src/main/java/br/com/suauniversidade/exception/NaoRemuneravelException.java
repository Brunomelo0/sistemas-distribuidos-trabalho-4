package br.com.suauniversidade.exception;

public class NaoRemuneravelException extends RuntimeException {
    public NaoRemuneravelException(String mensagem) {
        super(mensagem);
    }
}
