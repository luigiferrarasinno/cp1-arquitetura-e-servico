package br.edu.fiap.soa.estacionamento.web;

import br.edu.fiap.soa.estacionamento.domain.EstacionamentoConfig;
import br.edu.fiap.soa.estacionamento.service.EstacionamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/estacionamento")
@Tag(name = "Estacionamento", description = "Configuração e status do estacionamento")
public class EstacionamentoController {
    
    private final EstacionamentoService estacionamentoService;
    
    public EstacionamentoController(EstacionamentoService estacionamentoService) {
        this.estacionamentoService = estacionamentoService;
    }
    
    @GetMapping("/status")
    @Operation(summary = "Status atual do estacionamento")
    public EstacionamentoStatusDTO getStatus() {
        return EstacionamentoStatusDTO.builder()
                .totalVagas(estacionamentoService.getTotalVagas())
                .vagasOcupadas(estacionamentoService.getVagasOcupadas())
                .vagasLivres(estacionamentoService.getVagasLivres())
                .taxaOcupacao(estacionamentoService.getTaxaOcupacao())
                .lotado(estacionamentoService.isLotado())
                .build();
    }
    
    @GetMapping("/configuracao")
    @Operation(summary = "Obter configuração atual")
    public EstacionamentoConfig getConfiguracao() {
        return estacionamentoService.getConfiguracaoAtiva();
    }
    
    @PostMapping("/configuracao")
    @Operation(summary = "Atualizar configuração do estacionamento")
    public EstacionamentoConfig atualizarConfiguracao(@RequestBody ConfiguracaoRequest request) {
        return estacionamentoService.salvarConfiguracao(
                request.getTotalVagas(),
                request.getTarifa30Min(),
                request.getTarifaHora(),
                request.getTarifaDiaria(),
                request.getTarifaMensal()
        );
    }
    
    // DTOs internos
    @lombok.Data
    @lombok.Builder
    public static class EstacionamentoStatusDTO {
        private Integer totalVagas;
        private Long vagasOcupadas;
        private Long vagasLivres;
        private Double taxaOcupacao;
        private Boolean lotado;
    }
    
    @lombok.Data
    public static class ConfiguracaoRequest {
        private Integer totalVagas;
        private BigDecimal tarifa30Min;
        private BigDecimal tarifaHora;
        private BigDecimal tarifaDiaria;
        private BigDecimal tarifaMensal;
    }
}
