package br.edu.fiap.soa.estacionamento.web.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RelatorioOcupacaoDTO {
    private Integer totalVagas;
    private Long vagasOcupadas;
    private Long vagasLivres;
    private Double taxaOcupacao;
    private Boolean lotado;
}
