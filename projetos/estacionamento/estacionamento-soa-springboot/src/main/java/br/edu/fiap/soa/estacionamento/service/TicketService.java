package br.edu.fiap.soa.estacionamento.service;
import br.edu.fiap.soa.estacionamento.domain.*; 
import br.edu.fiap.soa.estacionamento.repository.*;
import org.springframework.beans.factory.annotation.Value; 
import org.springframework.stereotype.Service; 
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal; 
import java.math.RoundingMode; 
import java.time.Duration; 
import java.time.LocalDateTime; 
import java.util.List;
import java.util.Optional;

/**
 * Service refatorado responsável pelo gerenciamento de tickets
 */
@Service
public class TicketService {
    private final TicketRepository ticketRepo; 
    private final VeiculoRepository veiculoRepo;
    private final ReservaRepository reservaRepo;
    private final EstacionamentoService estacionamentoService;
    private final TarifaService tarifaService;
    
    @Value("${parking.hourly-rate:8.00}") 
    private BigDecimal hourlyRate;
    
    public TicketService(TicketRepository ticketRepo, 
                        VeiculoRepository veiculoRepo,
                        ReservaRepository reservaRepo,
                        EstacionamentoService estacionamentoService,
                        TarifaService tarifaService) {
        this.ticketRepo = ticketRepo;
        this.veiculoRepo = veiculoRepo;
        this.reservaRepo = reservaRepo;
        this.estacionamentoService = estacionamentoService;
        this.tarifaService = tarifaService;
    }
    
    public void setHourlyRate(BigDecimal rate) { 
        this.hourlyRate = rate; 
    }
    
    public Ticket obter(Long id) { 
        return ticketRepo.findById(id).orElseThrow(() -> 
            new RuntimeException("Ticket não encontrado")); 
    }
    
    public List<Ticket> listarAbertos() { 
        return ticketRepo.findByStatus(TicketStatus.ABERTO); 
    }
    
    @Transactional 
    public Ticket checkIn(String placa, String modelo, String cor, String vaga, TipoTarifa tipoTarifa) {
        // Verifica lotação
        if (estacionamentoService.isLotado()) {
            throw new RuntimeException("Estacionamento lotado - " + 
                estacionamentoService.getVagasOcupadas() + "/" + 
                estacionamentoService.getTotalVagas() + " vagas ocupadas");
        }
        
        // Busca ou cria o veículo
        Veiculo veiculo = veiculoRepo.findByPlaca(placa)
            .orElseGet(() -> criarNovoVeiculo(placa, modelo, cor));

        // Impede ticket ABERTO duplicado para o mesmo veículo
        if (ticketRepo.existsByVeiculoIdAndStatus(veiculo.getId(), TicketStatus.ABERTO)) {
            throw new RuntimeException("Já existe um ticket ABERTO para este veículo");
        }
        
        // Verifica se há reserva ativa para o veículo
        Reserva reserva = verificarReservaAtiva(veiculo.getId(), vaga);
        
        // Define tipo de tarifa padrão se não informado
        if (tipoTarifa == null) {
            tipoTarifa = TipoTarifa.HORARIA;
        }

        Ticket ticket = Ticket.builder()
            .veiculo(veiculo)
            .vaga(vaga)
            .entrada(LocalDateTime.now())
            .status(TicketStatus.ABERTO)
            .tipoTarifa(tipoTarifa)
            .reserva(reserva)
            .build();
        
        Ticket ticketSalvo = ticketRepo.save(ticket);
        
        // Se havia reserva, marca como utilizada
        if (reserva != null) {
            reserva.setStatus(StatusReserva.UTILIZADA);
            reservaRepo.save(reserva);
        }
        
        return ticketSalvo;
    }
    
    // Método com compatibilidade para chamadas antigas
    @Transactional 
    public Ticket checkIn(String placa, String modelo, String cor, String vaga) {
        return checkIn(placa, modelo, cor, vaga, TipoTarifa.HORARIA);
    }
    
    @Transactional 
    public Ticket checkOut(Long ticketId) {
        Ticket ticket = obter(ticketId);
        
        if (ticket.getStatus() == TicketStatus.FECHADO) {
            throw new RuntimeException("Ticket já está fechado");
        }
        
        LocalDateTime saida = LocalDateTime.now();
        ticket.setSaida(saida);
        
        // Calcula valor usando o novo sistema de tarifas
        BigDecimal valor = tarifaService.calcularValor(ticket.getEntrada(), saida, ticket.getTipoTarifa());
        ticket.setValor(valor);
        ticket.setStatus(TicketStatus.FECHADO);
        
        return ticketRepo.save(ticket);
    }
    
    // Método legado mantido para compatibilidade
    public BigDecimal calcularValor(LocalDateTime entrada, LocalDateTime saida) {
        return tarifaService.calcularValor(entrada, saida, TipoTarifa.HORARIA);
    }
    
    private Veiculo criarNovoVeiculo(String placa, String modelo, String cor) {
        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca(placa);
        veiculo.setModelo(modelo);
        veiculo.setCor(cor);
        return veiculoRepo.save(veiculo);
    }
    
    private Reserva verificarReservaAtiva(Long veiculoId, String vaga) {
        List<Reserva> reservas = reservaRepo.findByVeiculoIdAndStatus(veiculoId, StatusReserva.ATIVA);
        
        LocalDateTime agora = LocalDateTime.now();
        
        return reservas.stream()
            .filter(r -> r.getVaga().equals(vaga))
            .filter(r -> !agora.isBefore(r.getDataInicio()) && !agora.isAfter(r.getDataFim()))
            .findFirst()
            .orElse(null);
    }
}
