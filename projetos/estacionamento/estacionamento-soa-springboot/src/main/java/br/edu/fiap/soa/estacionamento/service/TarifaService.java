package br.edu.fiap.soa.estacionamento.service;

import br.edu.fiap.soa.estacionamento.domain.EstacionamentoConfig;
import br.edu.fiap.soa.estacionamento.domain.TipoTarifa;
import br.edu.fiap.soa.estacionamento.repository.EstacionamentoConfigRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Service responsável pelos cálculos de tarifas
 */
@Service
public class TarifaService {
    
    private final EstacionamentoConfigRepository configRepository;
    
    public TarifaService(EstacionamentoConfigRepository configRepository) {
        this.configRepository = configRepository;
    }
    
    /**
     * Calcula o valor baseado no tipo de tarifa
     */
    public BigDecimal calcularValor(LocalDateTime entrada, LocalDateTime saida, TipoTarifa tipoTarifa) {
        EstacionamentoConfig config = configRepository.findActiveConfig()
                .orElseThrow(() -> new RuntimeException("Configuração do estacionamento não encontrada"));
        
        Duration duracao = Duration.between(entrada, saida);
        
        return switch (tipoTarifa) {
            case FRACAO_30MIN -> calcularPorFracao30Min(duracao, config.getTarifa30Min());
            case HORARIA -> calcularPorHora(duracao, config.getTarifaHora());
            case DIARIA -> calcularPorDiaria(duracao, config.getTarifaDiaria(), config.getTarifaHora());
            case MENSAL -> config.getTarifaMensal(); // Valor fixo mensal
        };
    }
    
    private BigDecimal calcularPorFracao30Min(Duration duracao, BigDecimal tarifa30Min) {
        long minutos = duracao.toMinutes();
        long fracoes = (minutos + 29) / 30; // Arredonda para cima
        if (fracoes <= 0) fracoes = 1;
        
        return tarifa30Min.multiply(BigDecimal.valueOf(fracoes)).setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calcularPorHora(Duration duracao, BigDecimal tarifaHora) {
        long minutos = duracao.toMinutes();
        long horas = (minutos + 59) / 60; // Arredonda para cima
        if (horas <= 0) horas = 1;
        
        return tarifaHora.multiply(BigDecimal.valueOf(horas)).setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calcularPorDiaria(Duration duracao, BigDecimal tarifaDiaria, BigDecimal tarifaHora) {
        long horas = duracao.toHours();
        
        if (horas >= 24) {
            // Se ficar mais de 24h, cobra diária + horas extras
            long diasCompletos = horas / 24;
            long horasExtras = horas % 24;
            
            BigDecimal valorDiarias = tarifaDiaria.multiply(BigDecimal.valueOf(diasCompletos));
            BigDecimal valorHorasExtras = tarifaHora.multiply(BigDecimal.valueOf(horasExtras));
            
            return valorDiarias.add(valorHorasExtras).setScale(2, RoundingMode.HALF_UP);
        } else {
            // Menos de 24h, compara diária vs. por hora
            BigDecimal valorHorario = calcularPorHora(duracao, tarifaHora);
            return valorHorario.compareTo(tarifaDiaria) <= 0 ? valorHorario : tarifaDiaria;
        }
    }
    
    /**
     * Sugere o melhor tipo de tarifa baseado na duração estimada
     */
    public TipoTarifa sugerirMelhorTarifa(Duration duracaoEstimada) {
        long horas = duracaoEstimada.toHours();
        long minutos = duracaoEstimada.toMinutes();
        
        if (minutos <= 60) {
            return TipoTarifa.FRACAO_30MIN;
        } else if (horas <= 10) {
            return TipoTarifa.HORARIA;
        } else {
            return TipoTarifa.DIARIA;
        }
    }
}
