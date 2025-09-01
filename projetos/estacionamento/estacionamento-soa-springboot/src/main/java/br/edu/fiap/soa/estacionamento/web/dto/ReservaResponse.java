package br.edu.fiap.soa.estacionamento.web.dto;

import br.edu.fiap.soa.estacionamento.domain.StatusReserva;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReservaResponse {
    private Long id;
    private String placa;
    private String vaga;
    private LocalDateTime dataReserva;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private StatusReserva status;
}
