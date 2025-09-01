package br.edu.fiap.soa.estacionamento.web;

import br.edu.fiap.soa.estacionamento.service.RelatorioService;
import br.edu.fiap.soa.estacionamento.web.dto.RelatorioOcupacaoDTO;
import br.edu.fiap.soa.estacionamento.web.dto.RelatorioReceitaDTO;
import br.edu.fiap.soa.estacionamento.web.dto.RelatorioVagasDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/relatorios")
@Tag(name = "Relatórios", description = "Relatórios de ocupação, receita e uso de vagas")
public class RelatorioController {
    
    private final RelatorioService relatorioService;
    
    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }
    
    @GetMapping("/ocupacao")
    @Operation(summary = "Relatório de ocupação atual")
    public RelatorioOcupacaoDTO getRelatorioOcupacao() {
        return relatorioService.getRelatorioOcupacao();
    }
    
    @GetMapping("/receita")
    @Operation(summary = "Relatório de receita por período")
    public RelatorioReceitaDTO getRelatorioReceita(
            @Parameter(description = "Data de início", example = "2025-09-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @Parameter(description = "Data de fim", example = "2025-09-01T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return relatorioService.getRelatorioReceita(inicio, fim);
    }
    
    @GetMapping("/vagas")
    @Operation(summary = "Relatório de uso das vagas por período")
    public RelatorioVagasDTO getRelatorioVagas(
            @Parameter(description = "Data de início", example = "2025-09-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @Parameter(description = "Data de fim", example = "2025-09-01T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return relatorioService.getRelatorioVagas(inicio, fim);
    }
    
    @GetMapping("/consolidado")
    @Operation(summary = "Relatório consolidado")
    public Map<String, Object> getRelatorioConsolidado(
            @Parameter(description = "Data de início", example = "2025-09-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @Parameter(description = "Data de fim", example = "2025-09-01T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return relatorioService.getRelatorioConsolidado(inicio, fim);
    }
}
