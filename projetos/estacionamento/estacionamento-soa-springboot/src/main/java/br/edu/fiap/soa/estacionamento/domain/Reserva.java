package br.edu.fiap.soa.estacionamento.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidade para representar reservas de vagas
 */
@Entity
@Table(name = "reserva")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reserva_veiculo"))
    private Veiculo veiculo;
    
    @Column(length = 10, nullable = false)
    private String vaga;
    
    @Column(name = "data_reserva", nullable = false)
    private LocalDateTime dataReserva;
    
    @Column(name = "data_inicio", nullable = false)
    private LocalDateTime dataInicio;
    
    @Column(name = "data_fim", nullable = false)
    private LocalDateTime dataFim;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private StatusReserva status;
    
    @PrePersist
    public void prePersist() {
        if (dataReserva == null) dataReserva = LocalDateTime.now();
        if (status == null) status = StatusReserva.ATIVA;
    }
}
