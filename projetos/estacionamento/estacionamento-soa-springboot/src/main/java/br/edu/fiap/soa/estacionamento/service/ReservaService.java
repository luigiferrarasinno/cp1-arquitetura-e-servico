package br.edu.fiap.soa.estacionamento.service;

import br.edu.fiap.soa.estacionamento.domain.*;
import br.edu.fiap.soa.estacionamento.repository.ReservaRepository;
import br.edu.fiap.soa.estacionamento.repository.VeiculoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service responsável pelo gerenciamento de reservas
 */
@Service
public class ReservaService {
    
    private final ReservaRepository reservaRepository;
    private final VeiculoRepository veiculoRepository;
    private final EstacionamentoService estacionamentoService;
    
    public ReservaService(ReservaRepository reservaRepository, 
                         VeiculoRepository veiculoRepository,
                         EstacionamentoService estacionamentoService) {
        this.reservaRepository = reservaRepository;
        this.veiculoRepository = veiculoRepository;
        this.estacionamentoService = estacionamentoService;
    }
    
    @Transactional
    public Reserva criarReserva(String placa, String vaga, LocalDateTime dataInicio, LocalDateTime dataFim) {
        // Validações
        if (dataInicio.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Data de início não pode ser no passado");
        }
        
        if (dataFim.isBefore(dataInicio)) {
            throw new RuntimeException("Data de fim deve ser posterior à data de início");
        }
        
        // Verifica se a vaga está disponível no período
        List<Reserva> conflitos = reservaRepository.findReservasConflitantes(vaga, dataInicio, dataFim);
        if (!conflitos.isEmpty()) {
            throw new RuntimeException("Vaga já reservada no período solicitado");
        }
        
        // Busca ou cria o veículo
        Veiculo veiculo = veiculoRepository.findByPlaca(placa)
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado. Cadastre primeiro."));
        
        // Cria a reserva
        Reserva reserva = Reserva.builder()
                .veiculo(veiculo)
                .vaga(vaga)
                .dataInicio(dataInicio)
                .dataFim(dataFim)
                .status(StatusReserva.ATIVA)
                .build();
        
        return reservaRepository.save(reserva);
    }
    
    @Transactional
    public void cancelarReserva(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RuntimeException("Reserva não encontrada"));
        
        if (reserva.getStatus() != StatusReserva.ATIVA) {
            throw new RuntimeException("Apenas reservas ativas podem ser canceladas");
        }
        
        reserva.setStatus(StatusReserva.CANCELADA);
        reservaRepository.save(reserva);
    }
    
    @Transactional
    public void utilizarReserva(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RuntimeException("Reserva não encontrada"));
        
        if (reserva.getStatus() != StatusReserva.ATIVA) {
            throw new RuntimeException("Reserva não está ativa");
        }
        
        LocalDateTime agora = LocalDateTime.now();
        if (agora.isBefore(reserva.getDataInicio()) || agora.isAfter(reserva.getDataFim())) {
            throw new RuntimeException("Reserva fora do período válido");
        }
        
        reserva.setStatus(StatusReserva.UTILIZADA);
        reservaRepository.save(reserva);
    }
    
    public List<Reserva> listarReservasAtivas() {
        return reservaRepository.findByStatus(StatusReserva.ATIVA);
    }
    
    public List<Reserva> listarReservasPorVeiculo(Long veiculoId) {
        return reservaRepository.findByVeiculoIdAndStatus(veiculoId, StatusReserva.ATIVA);
    }
    
    /**
     * Job para expirar reservas automaticamente
     */
    @Scheduled(fixedRate = 300000) // A cada 5 minutos
    @Transactional
    public void expirarReservas() {
        List<Reserva> reservasExpiradas = reservaRepository.findReservasExpiradas(LocalDateTime.now());
        
        for (Reserva reserva : reservasExpiradas) {
            reserva.setStatus(StatusReserva.EXPIRADA);
            reservaRepository.save(reserva);
        }
    }
}
