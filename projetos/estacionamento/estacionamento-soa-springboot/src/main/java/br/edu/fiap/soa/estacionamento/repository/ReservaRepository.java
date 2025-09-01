package br.edu.fiap.soa.estacionamento.repository;

import br.edu.fiap.soa.estacionamento.domain.Reserva;
import br.edu.fiap.soa.estacionamento.domain.StatusReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    
    List<Reserva> findByStatus(StatusReserva status);
    
    @Query("SELECT r FROM Reserva r WHERE r.vaga = :vaga AND r.status = 'ATIVA' " +
           "AND :dataInicio < r.dataFim AND :dataFim > r.dataInicio")
    List<Reserva> findReservasConflitantes(@Param("vaga") String vaga, 
                                          @Param("dataInicio") LocalDateTime dataInicio,
                                          @Param("dataFim") LocalDateTime dataFim);
    
    @Query("SELECT r FROM Reserva r WHERE r.status = 'ATIVA' AND r.dataFim < :now")
    List<Reserva> findReservasExpiradas(@Param("now") LocalDateTime now);
    
    List<Reserva> findByVeiculoIdAndStatus(Long veiculoId, StatusReserva status);
}
