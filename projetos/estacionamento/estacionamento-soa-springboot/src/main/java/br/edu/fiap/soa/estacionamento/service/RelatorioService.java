package br.edu.fiap.soa.estacionamento.service;

import br.edu.fiap.soa.estacionamento.domain.Ticket;
import br.edu.fiap.soa.estacionamento.domain.TicketStatus;
import br.edu.fiap.soa.estacionamento.repository.TicketRepository;
import br.edu.fiap.soa.estacionamento.web.dto.RelatorioOcupacaoDTO;
import br.edu.fiap.soa.estacionamento.web.dto.RelatorioReceitaDTO;
import br.edu.fiap.soa.estacionamento.web.dto.RelatorioVagasDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service responsável por gerar relatórios
 */
@Service
public class RelatorioService {
    
    private final TicketRepository ticketRepository;
    private final EstacionamentoService estacionamentoService;
    
    public RelatorioService(TicketRepository ticketRepository,
                          EstacionamentoService estacionamentoService) {
        this.ticketRepository = ticketRepository;
        this.estacionamentoService = estacionamentoService;
    }
    
    /**
     * Relatório de receita por período
     */
    public RelatorioReceitaDTO getRelatorioReceita(LocalDateTime inicio, LocalDateTime fim) {
        List<Ticket> tickets = ticketRepository.findByPeriodo(inicio, fim);
        
        BigDecimal receitaTotal = tickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.FECHADO && t.getValor() != null)
                .map(Ticket::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long totalTickets = tickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.FECHADO)
                .count();
          BigDecimal ticketMedio = totalTickets > 0 
                ? receitaTotal.divide(BigDecimal.valueOf(totalTickets), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        
        return RelatorioReceitaDTO.builder()
                .periodo(inicio + " até " + fim)
                .receitaTotal(receitaTotal)
                .totalTickets(totalTickets)
                .ticketMedio(ticketMedio)
                .build();
    }
    
    /**
     * Relatório de ocupação atual
     */
    public RelatorioOcupacaoDTO getRelatorioOcupacao() {
        return RelatorioOcupacaoDTO.builder()
                .totalVagas(estacionamentoService.getTotalVagas())
                .vagasOcupadas(estacionamentoService.getVagasOcupadas())
                .vagasLivres(estacionamentoService.getVagasLivres())
                .taxaOcupacao(estacionamentoService.getTaxaOcupacao())
                .lotado(estacionamentoService.isLotado())
                .build();
    }
    
    /**
     * Relatório de uso das vagas
     */
    public RelatorioVagasDTO getRelatorioVagas(LocalDateTime inicio, LocalDateTime fim) {
        List<Object[]> dadosVagas = ticketRepository.getRelatorioUsoVagas(inicio, fim);
        
        Map<String, Long> usoVagas = new HashMap<>();
        for (Object[] linha : dadosVagas) {
            String vaga = (String) linha[0];
            Long count = ((Number) linha[1]).longValue();
            usoVagas.put(vaga, count);
        }
        
        // Vaga mais utilizada
        String vagaMaisUsada = usoVagas.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
        
        // Vaga menos utilizada
        String vagaMenosUsada = usoVagas.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
        
        return RelatorioVagasDTO.builder()
                .periodo(inicio + " até " + fim)
                .usoVagas(usoVagas)
                .vagaMaisUsada(vagaMaisUsada)
                .vagaMenosUsada(vagaMenosUsada)
                .totalUsos(usoVagas.values().stream().mapToLong(Long::longValue).sum())
                .build();
    }
    
    /**
     * Relatório consolidado
     */
    public Map<String, Object> getRelatorioConsolidado(LocalDateTime inicio, LocalDateTime fim) {
        Map<String, Object> relatorio = new HashMap<>();
        
        relatorio.put("receita", getRelatorioReceita(inicio, fim));
        relatorio.put("ocupacao", getRelatorioOcupacao());
        relatorio.put("vagas", getRelatorioVagas(inicio, fim));
        
        return relatorio;
    }
}
