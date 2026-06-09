package br.com.suauniversidade.mensageria;

import java.util.Map;

public record EventoDominio(String topico, Map<String, Object> dados) {
}
