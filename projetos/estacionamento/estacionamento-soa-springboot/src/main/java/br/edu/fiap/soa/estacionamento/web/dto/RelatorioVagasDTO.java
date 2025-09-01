package br.edu.fiap.soa.estacionamento.web.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class RelatorioVagasDTO {
    private String periodo;
    private Map<String, Long> usoVagas;
    private String vagaMaisUsada;
    private String vagaMenosUsada;
    private Long totalUsos;
}
