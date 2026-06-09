package br.com.suauniversidade.repository;

import br.com.suauniversidade.model.Curso;
import br.com.suauniversidade.model.Departamento;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
public class RepositorioObjetos {

    private final Map<String, Departamento> departamentos = new ConcurrentHashMap<>();
    private final Map<String, Curso> cursos = new ConcurrentHashMap<>();
    private final AtomicInteger contador = new AtomicInteger(0);

    public Departamento registrarDepartamento(Departamento departamento) {
        String id = "departamento-" + contador.incrementAndGet();
        departamento.setId(id);
        departamentos.put(id, departamento);
        return departamento;
    }

    public Curso registrarCurso(Curso curso) {
        String id = "curso-" + contador.incrementAndGet();
        curso.setId(id);
        cursos.put(id, curso);
        return curso;
    }

    public Departamento resolverDepartamento(String id) {
        return departamentos.get(id);
    }

    public Curso resolverCurso(String id) {
        return cursos.get(id);
    }
}
