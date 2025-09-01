package br.edu.fiap.soa.estacionamento.web.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RelatorioReceitaDTO {
    private String periodo;
    private BigDecimal receitaTotal;
    private Long totalTickets;
    private BigDecimal ticketMedio;
}
