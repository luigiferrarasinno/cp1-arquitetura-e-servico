package br.edu.fiap.soa.estacionamento.service;

import br.edu.fiap.soa.estacionamento.domain.EstacionamentoConfig;
import br.edu.fiap.soa.estacionamento.repository.EstacionamentoConfigRepository;
import br.edu.fiap.soa.estacionamento.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service responsável pelo controle geral do estacionamento
 */
@Service
public class EstacionamentoService {
    
    private final EstacionamentoConfigRepository configRepository;
    private final TicketRepository ticketRepository;
    
    public EstacionamentoService(EstacionamentoConfigRepository configRepository,
                               TicketRepository ticketRepository) {
        this.configRepository = configRepository;
        this.ticketRepository = ticketRepository;
    }
    
    /**
     * Verifica se há vagas disponíveis
     */
    public boolean temVagasDisponiveis() {
        EstacionamentoConfig config = getConfiguracaoAtiva();
        Long ticketsAbertos = ticketRepository.countTicketsAbertos();
        
        return ticketsAbertos < config.getTotalVagas();
    }
    
    /**
     * Retorna o número de vagas ocupadas
     */
    public Long getVagasOcupadas() {
        return ticketRepository.countTicketsAbertos();
    }
    
    /**
     * Retorna o número total de vagas
     */
    public Integer getTotalVagas() {
        return getConfiguracaoAtiva().getTotalVagas();
    }
    
    /**
     * Retorna o número de vagas livres
     */
    public Long getVagasLivres() {
        return getTotalVagas() - getVagasOcupadas();
    }
    
    /**
     * Calcula a taxa de ocupação em percentual
     */
    public Double getTaxaOcupacao() {
        Long ocupadas = getVagasOcupadas();
        Integer total = getTotalVagas();
        
        if (total == 0) return 0.0;
        
        return (ocupadas.doubleValue() / total.doubleValue()) * 100.0;
    }
    
    /**
     * Verifica se o estacionamento está lotado
     */
    public boolean isLotado() {
        return !temVagasDisponiveis();
    }
    
    /**
     * Retorna a configuração ativa do estacionamento
     */
    public EstacionamentoConfig getConfiguracaoAtiva() {
        return configRepository.findActiveConfig()
                .orElseThrow(() -> new RuntimeException("Configuração do estacionamento não encontrada"));
    }
    
    /**
     * Cria ou atualiza a configuração do estacionamento
     */
    @Transactional
    public EstacionamentoConfig salvarConfiguracao(Integer totalVagas, 
                                                 BigDecimal tarifa30Min,
                                                 BigDecimal tarifaHora,
                                                 BigDecimal tarifaDiaria,
                                                 BigDecimal tarifaMensal) {
        // Desativa configurações antigas
        configRepository.findActiveConfig().ifPresent(config -> {
            config.setAtivo(false);
            configRepository.save(config);
        });
        
        // Cria nova configuração
        EstacionamentoConfig novaConfig = EstacionamentoConfig.builder()
                .totalVagas(totalVagas)
                .tarifa30Min(tarifa30Min)
                .tarifaHora(tarifaHora)
                .tarifaDiaria(tarifaDiaria)
                .tarifaMensal(tarifaMensal)
                .ativo(true)
                .build();
        
        return configRepository.save(novaConfig);
    }
}
