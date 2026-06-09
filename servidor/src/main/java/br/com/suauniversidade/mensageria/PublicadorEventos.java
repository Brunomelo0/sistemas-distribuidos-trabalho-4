package br.com.suauniversidade.mensageria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

@Component
public class PublicadorEventos {

    private final String urlBroker;
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final LinkedBlockingDeque<EventoDominio> outbox = new LinkedBlockingDeque<>();

    public PublicadorEventos(@Value("${broker.url:http://localhost:9000}") String urlBroker,
                             ObjectMapper mapper) {
        this.urlBroker = urlBroker;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    public void publicar(String topico, Map<String, Object> dados) {
        outbox.offerLast(new EventoDominio(topico, dados));
    }

    @Scheduled(fixedDelay = 500)
    public void drenarOutbox() {
        EventoDominio evento;
        while ((evento = outbox.pollFirst()) != null) {
            if (!enviar(evento)) {
                outbox.offerFirst(evento);
                break;
            }
        }
    }

    private boolean enviar(EventoDominio evento) {
        try {
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("topico", evento.topico());
            corpo.put("dados", evento.dados());
            String json = mapper.writeValueAsString(corpo);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(urlBroker + "/publicar"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(2))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            boolean ok = resp.statusCode() >= 200 && resp.statusCode() < 300;
            if (ok) {
                System.out.printf("  [publicador] evento \"%s\" publicado no broker%n", evento.topico());
            }
            return ok;
        } catch (Exception e) {
            System.out.printf("  [publicador] broker indisponivel; evento \"%s\" aguardando na outbox%n",
                    evento.topico());
            return false;
        }
    }
}
