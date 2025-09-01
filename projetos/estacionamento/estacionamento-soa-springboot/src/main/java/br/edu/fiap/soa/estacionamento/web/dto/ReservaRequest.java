package br.edu.fiap.soa.estacionamento.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Dados para criação de reserva")
public class ReservaRequest {
    @Schema(example = "ABC-1D23")
    @NotBlank
    private String placa;
    
    @Schema(example = "A12")
    @NotBlank
    private String vaga;
    
    @Schema(example = "2025-09-02T10:00:00")
    @NotNull
    private LocalDateTime dataInicio;
    
    @Schema(example = "2025-09-02T18:00:00")
    @NotNull
    private LocalDateTime dataFim;
}
