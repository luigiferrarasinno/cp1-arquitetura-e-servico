# 📋 **CP1 - CHECKPOINT 1: REFATORAÇÃO E MELHORIAS DO SISTEMA DE ESTACIONAMENTO**

**Professor:** Salatiel Luz Marinho  
**Data:** 01/09/2025  
**Disciplina:** SOA (Arquitetura Orientada a Serviços)  
**Aluno:** Luigi Ferrara 
**RM:** rm98047

---

## 🎯 **OBJETIVO DO CHECKPOINT**

Este checkpoint teve como objetivo refatorar e expandir o sistema básico de estacionamento, implementando funcionalidades avançadas e aplicando boas práticas de arquitetura de software. As melhorias incluem sistema de reservas, controle de lotação, múltiplas tarifas e relatórios gerenciais.

---

## 📊 **RESUMO EXECUTIVO DAS MELHORIAS**

| **Categoria** | **Antes** | **Depois** |
|---------------|-----------|------------|
| **Endpoints API** | 4 básicos | 15+ endpoints |
| **Tipos de Tarifa** | 1 (apenas hora) | 4 tipos diferentes |
| **Entidades JPA** | 2 entidades | 5 entidades |
| **Services** | 2 services | 6 services especializados |
| **Relatórios** | 0 relatórios | 4 tipos de relatórios |
| **Funcionalidades** | CRUD básico | Sistema enterprise |

---

## 🔄 **1. ANÁLISE DO CÓDIGO ORIGINAL E REFATORAÇÕES APLICADAS**

### **1.1. Problemas Identificados no Código Original**

#### **❌ Responsabilidade Múltipla**
```java
// ANTES: TicketService criava veículos (violava SRP)
@Transactional public Ticket checkIn(String placa, String modelo, String cor, String vaga){
    Veiculo veiculo = veiculoRepo.findByPlaca(placa).orElseGet(()-> { 
        Veiculo nv = new Veiculo(); 
        nv.setPlaca(placa); 
        nv.setModelo(modelo); 
        nv.setCor(cor); 
        return veiculoRepo.save(nv); 
    });
    // ...
}
```

#### **✅ Solução Aplicada**
```java
// DEPOIS: Responsabilidades separadas
@Service
public class TicketService {
    // Foca apenas em tickets
    private final TarifaService tarifaService;
    private final EstacionamentoService estacionamentoService;
    // ...
}

// Service especializado para cálculos
@Service  
public class TarifaService {
    public BigDecimal calcularValor(LocalDateTime entrada, LocalDateTime saida, TipoTarifa tipoTarifa) {
        // Lógica especializada de cálculo
    }
}
```

#### **❌ Lógica de Negócio Limitada**
```java
// ANTES: Apenas cálculo simples por hora
public BigDecimal calcularValor(LocalDateTime entrada, LocalDateTime saida){
    long minutes = Duration.between(entrada, saida).toMinutes(); 
    long hours = (minutes + 59)/60; 
    if(hours<=0) hours=1;
    return hourlyRate.multiply(BigDecimal.valueOf(hours)).setScale(2, RoundingMode.HALF_UP);
}
```

#### **✅ Solução Aplicada**
```java
// DEPOIS: Sistema flexível com múltiplas tarifas
public BigDecimal calcularValor(LocalDateTime entrada, LocalDateTime saida, TipoTarifa tipoTarifa) {
    return switch (tipoTarifa) {
        case FRACAO_30MIN -> calcularPorFracao30Min(duracao, config.getTarifa30Min());
        case HORARIA -> calcularPorHora(duracao, config.getTarifaHora());
        case DIARIA -> calcularPorDiaria(duracao, config.getTarifaDiaria(), config.getTarifaHora());
        case MENSAL -> config.getTarifaMensal();
    };
}
```

---

## 🆕 **2. NOVAS FUNCIONALIDADES IMPLEMENTADAS**

### **2.1. Sistema de Reservas de Vagas** ✅

#### **Entidades Criadas:**
- **`Reserva.java`** - Entidade principal para reservas
- **`StatusReserva.java`** - Enum para controle de status (ATIVA, UTILIZADA, CANCELADA, EXPIRADA)

#### **Funcionalidades:**
- ✅ **Criação de reservas** por período específico
- ✅ **Verificação de conflitos** de horário/vaga
- ✅ **Cancelamento de reservas** ativas
- ✅ **Expiração automática** via job scheduled (a cada 15 min)
- ✅ **Integração com check-in** (reserva utilizada automaticamente)

#### **Endpoints Novos:**
```bash
POST   /api/reservas                    # Criar reserva
GET    /api/reservas/ativas             # Listar reservas ativas  
GET    /api/reservas/veiculo/{id}       # Reservas por veículo
POST   /api/reservas/{id}/cancelar      # Cancelar reserva
POST   /api/reservas/{id}/utilizar      # Marcar como utilizada
```

### **2.2. Controle Inteligente de Lotação** ✅

#### **Implementações:**
- **`EstacionamentoConfig.java`** - Configuração de vagas totais
- **`EstacionamentoService.java`** - Controle inteligente de lotação

#### **Funcionalidades:**
- ✅ **Validação antes do check-in** (impede entrada se lotado)
- ✅ **Métricas em tempo real** (vagas ocupadas/livres)
- ✅ **Taxa de ocupação** percentual
- ✅ **Status de lotação** booleano

#### **Exemplo de Validação:**
```java
@Transactional 
public Ticket checkIn(String placa, String modelo, String cor, String vaga, TipoTarifa tipoTarifa) {
    // Verifica lotação ANTES de permitir entrada
    if (estacionamentoService.isLotado()) {
        throw new RuntimeException("Estacionamento lotado - " + 
            estacionamentoService.getVagasOcupadas() + "/" + 
            estacionamentoService.getTotalVagas() + " vagas ocupadas");
    }
    // ...
}
```

### **2.3. Sistema de Tarifas Diferenciadas** ✅

#### **Tipos Implementados:**

| **Tipo** | **Descrição** | **Estratégia de Cálculo** |
|----------|---------------|---------------------------|
| **FRACAO_30MIN** | Fração de 30 minutos | Arredonda para cima em blocos de 30min |
| **HORARIA** | Por hora completa (original) | Arredonda para cima por hora |
| **DIARIA** | Tarifa diária (até 24h) | Compara horário vs. diária, escolhe menor |
| **MENSAL** | Mensalista (valor fixo) | Valor fixo configurável |

#### **Exemplo de Cálculo Inteligente:**
```java
private BigDecimal calcularPorDiaria(Duration duracao, BigDecimal tarifaDiaria, BigDecimal tarifaHora) {
    long horas = duracao.toHours();
    
    if (horas >= 24) {
        // Mais de 24h: cobra diária + horas extras
        long diasCompletos = horas / 24;
        long horasExtras = horas % 24;
        
        BigDecimal valorDiarias = tarifaDiaria.multiply(BigDecimal.valueOf(diasCompletos));
        BigDecimal valorHorasExtras = tarifaHora.multiply(BigDecimal.valueOf(horasExtras));
        
        return valorDiarias.add(valorHorasExtras).setScale(2, RoundingMode.HALF_UP);
    } else {
        // Menos de 24h: compara diária vs. por hora (escolhe o menor)
        BigDecimal valorHorario = calcularPorHora(duracao, tarifaHora);
        return valorHorario.compareTo(tarifaDiaria) <= 0 ? valorHorario : tarifaDiaria;
    }
}
```

### **2.4. Sistema de Relatórios Gerenciais** ✅

#### **Relatórios Implementados:**

1. **📊 Relatório de Receita**
   - Receita total por período
   - Total de tickets processados  
   - Ticket médio (receita/quantidade)

2. **🏢 Relatório de Ocupação**
   - Status atual do estacionamento
   - Taxa de ocupação percentual
   - Vagas disponíveis/ocupadas

3. **🅿️ Relatório de Uso de Vagas**
   - Vagas mais/menos utilizadas
   - Estatísticas por vaga individual
   - Total de usos por período

4. **📈 Relatório Consolidado**
   - Combina todos os relatórios
   - Visão 360° do negócio

#### **Endpoints de Relatórios:**
```bash
GET /api/relatorios/ocupacao                           # Status atual
GET /api/relatorios/receita?inicio=&fim=               # Receita por período  
GET /api/relatorios/vagas?inicio=&fim=                 # Uso das vagas
GET /api/relatorios/consolidado?inicio=&fim=           # Relatório completo
```

---

## 🏗️ **3. NOVA ARQUITETURA DE SERVICES**

### **3.1. Services Especializados Criados**

```
📁 Services Refatorados:
├── TicketService.java (refatorado - foca apenas em tickets)
├── TarifaService.java (novo - cálculos especializados)  
├── ReservaService.java (novo - gerenciamento de reservas)
├── EstacionamentoService.java (novo - controle de lotação)
├── RelatorioService.java (novo - relatórios gerenciais)
└── VeiculoService.java (mantido - CRUD de veículos)
```

### **3.2. Benefícios da Nova Arquitetura**

- ✅ **Princípio da Responsabilidade Única** (SRP)
- ✅ **Facilidade de manutenção** e evolução
- ✅ **Testabilidade** individual dos services
- ✅ **Reusabilidade** dos componentes
- ✅ **Injeção de dependência** bem estruturada

---

## 🗃️ **4. MELHORIAS NO BANCO DE DADOS**

### **4.1. Novas Tabelas Criadas**

#### **Script: `03_melhorias.sql`**

```sql
-- 1. Configuração do estacionamento
CREATE TABLE estacionamento_config (
    id NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    total_vagas NUMBER NOT NULL,
    tarifa_30min NUMBER(10,2),
    tarifa_hora NUMBER(10,2), 
    tarifa_diaria NUMBER(10,2),
    tarifa_mensal NUMBER(10,2),
    ativo NUMBER(1) DEFAULT 1 NOT NULL
);

-- 2. Sistema de reservas
CREATE TABLE reserva (
    id NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    veiculo_id NUMBER NOT NULL,
    vaga VARCHAR2(10) NOT NULL,
    data_reserva TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    data_inicio TIMESTAMP NOT NULL,
    data_fim TIMESTAMP NOT NULL,
    status VARCHAR2(20) DEFAULT 'ATIVA' NOT NULL,
    CONSTRAINT fk_reserva_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculo(id)
);
```

### **4.2. Colunas Adicionadas em Tabelas Existentes**

```sql
-- Melhorias na tabela ticket
ALTER TABLE ticket ADD (
    tipo_tarifa VARCHAR2(20) DEFAULT 'HORARIA',
    reserva_id NUMBER
);

-- Constraint para relacionamento com reserva
ALTER TABLE ticket ADD CONSTRAINT fk_ticket_reserva 
    FOREIGN KEY (reserva_id) REFERENCES reserva(id);
```

### **4.3. Índices para Performance**

```sql
-- Índices otimizados para consultas frequentes
CREATE INDEX ix_reserva_status ON reserva(status);
CREATE INDEX ix_reserva_veiculo ON reserva(veiculo_id);
CREATE INDEX ix_reserva_periodo ON reserva(data_inicio, data_fim);
CREATE INDEX ix_ticket_tipo_tarifa ON ticket(tipo_tarifa);
```

### **4.4. Dados Iniciais**

```sql
-- Configuração padrão do estacionamento
INSERT INTO estacionamento_config 
(total_vagas, tarifa_30min, tarifa_hora, tarifa_diaria, tarifa_mensal, ativo) 
VALUES (100, 4.00, 8.00, 30.00, 200.00, 1);
```

---

## 📁 **5. ESTRUTURA DE PASTAS E ARQUIVOS CRIADOS**

### **5.1. Novos Domínios (Entidades)**
```
src/main/java/br/edu/fiap/soa/estacionamento/domain/
├── TipoTarifa.java (novo)
├── EstacionamentoConfig.java (novo)  
├── Reserva.java (novo)
├── StatusReserva.java (novo)
├── Ticket.java (modificado - novos campos)
├── Veiculo.java (mantido)
└── TicketStatus.java (mantido)
```

### **5.2. Novos Repositórios**
```
src/main/java/br/edu/fiap/soa/estacionamento/repository/
├── EstacionamentoConfigRepository.java (novo)
├── ReservaRepository.java (novo)
├── TicketRepository.java (expandido - novas consultas)
└── VeiculoRepository.java (mantido)
```

### **5.3. Novos Services**
```
src/main/java/br/edu/fiap/soa/estacionamento/service/
├── TarifaService.java (novo)
├── ReservaService.java (novo)
├── EstacionamentoService.java (novo)
├── RelatorioService.java (novo)
├── TicketService.java (refatorado)
└── VeiculoService.java (mantido)
```

### **5.4. Novos Controllers**
```
src/main/java/br/edu/fiap/soa/estacionamento/web/
├── ReservaController.java (novo)
├── RelatorioController.java (novo)
├── EstacionamentoController.java (novo)
├── TicketController.java (modificado)
└── VeiculoController.java (mantido)
```

### **5.5. Novos DTOs**
```
src/main/java/br/edu/fiap/soa/estacionamento/web/dto/
├── ReservaRequest.java (novo)
├── ReservaResponse.java (novo)
├── RelatorioReceitaDTO.java (novo)
├── RelatorioOcupacaoDTO.java (novo)
├── RelatorioVagasDTO.java (novo)
├── CheckInRequest.java (modificado - novo campo tipoTarifa)
├── TicketResponse.java (mantido)
└── VeiculoDTO.java (mantido)
```

### **5.6. Scripts SQL**
```
db/oracle/
├── 01_schema.sql (original)
├── 02_sample_data.sql (original)
└── 03_melhorias.sql (novo - todas as melhorias)
```

---

## 🌐 **6. NOVOS ENDPOINTS DA API**

### **6.1. Endpoints de Reservas**
```http
POST   /api/reservas                    # Criar nova reserva
GET    /api/reservas/ativas             # Listar todas as reservas ativas
GET    /api/reservas/veiculo/{id}       # Buscar reservas por veículo
POST   /api/reservas/{id}/cancelar      # Cancelar uma reserva
POST   /api/reservas/{id}/utilizar      # Marcar reserva como utilizada
```

### **6.2. Endpoints de Relatórios**
```http
GET    /api/relatorios/ocupacao         # Status atual do estacionamento
GET    /api/relatorios/receita          # Relatório de receita (query: inicio, fim)
GET    /api/relatorios/vagas            # Relatório de uso das vagas (query: inicio, fim)
GET    /api/relatorios/consolidado      # Relatório consolidado (query: inicio, fim)
```

### **6.3. Endpoints de Configuração**
```http
GET    /api/estacionamento/status       # Status e métricas atuais
GET    /api/estacionamento/configuracao # Obter configuração atual
POST   /api/estacionamento/configuracao # Atualizar configuração
```

### **6.4. Tickets Melhorados**
```http
POST   /api/tickets/checkin             # Check-in com suporte a tipos de tarifa
POST   /api/tickets/{id}/checkout       # Check-out com cálculo inteligente
GET    /api/tickets/abertos             # Listar tickets abertos
GET    /api/tickets/{id}                # Obter ticket específico
```

---

## 🔧 **7. JOBS AUTOMÁTICOS E SCHEDULERS**

### **7.1. Job de Expiração de Reservas**
```java
@Scheduled(fixedRate = 900000) // A cada 15 minutos
@Transactional
public void expirarReservasVencidas() {
    LocalDateTime agora = LocalDateTime.now();
    List<Reserva> reservasExpiradas = reservaRepository.findReservasExpiradas(agora);
    
    for (Reserva reserva : reservasExpiradas) {
        reserva.setStatus(StatusReserva.EXPIRADA);
        reservaRepository.save(reserva);
    }
}
```

### **7.2. Configuração de Scheduling**
```java
@SpringBootApplication
@EnableScheduling  // Habilitado no main
public class EstacionamentoApplication {
    // ...
}
```

---

## 📝 **8. EXEMPLOS DE USO DAS NOVAS FUNCIONALIDADES**

### **8.1. Check-in com Tarifa Diferenciada**
```json
POST /api/tickets/checkin
{
  "placa": "ABC-1234",
  "modelo": "Honda Civic",
  "cor": "Preto", 
  "vaga": "A10",
  "tipoTarifa": "FRACAO_30MIN"
}
```

### **8.2. Criação de Reserva**
```json
POST /api/reservas
{
  "placa": "ABC-1234",
  "vaga": "A10",
  "dataInicio": "2025-09-02T09:00:00",
  "dataFim": "2025-09-02T18:00:00"
}
```

### **8.3. Consulta de Status**
```json
GET /api/estacionamento/status

Response:
{
  "totalVagas": 100,
  "vagasOcupadas": 45,
  "vagasLivres": 55,
  "taxaOcupacao": 45.0,
  "lotado": false
}
```

### **8.4. Relatório de Receita**
```json
GET /api/relatorios/receita?inicio=2025-09-01T00:00:00&fim=2025-09-01T23:59:59

Response:
{
  "periodo": "2025-09-01T00:00 até 2025-09-01T23:59:59",
  "receitaTotal": 1250.00,
  "totalTickets": 78,
  "ticketMedio": 16.03
}
```

---

## 🎯 **9. TRÊS EVOLUÇÕES IDENTIFICADAS**

### **✅ 9.1. EVOLUÇÃO IMPLEMENTADA: Sistema de Reservas Inteligente**

**Descrição:** Sistema completo de reservas com validação de conflitos, expiração automática e integração perfeita com check-in.

**Benefícios Implementados:**
- ✅ Melhora significativa na experiência do usuário
- ✅ Redução do tempo de busca por vagas
- ✅ Permite planejamento antecipado de uso
- ✅ Integração automática com processo de check-in
- ✅ Limpeza automática de reservas expiradas

**Tecnologias Utilizadas:**
- Spring Data JPA para persistência
- Spring Scheduling para jobs automáticos
- Validações de negócio customizadas
- RESTful APIs bem estruturadas

### **🔄 9.2. PRÓXIMA EVOLUÇÃO: Sistema de Notificações**

**Proposta de Implementação:**
- **Notificações Push/Email** para expiração de reservas
- **Alertas de lotação** do estacionamento
- **Lembretes de check-out** próximo ao vencimento
- **Notificações de promoções** baseadas em uso

**Tecnologias Sugeridas:**
- Spring Boot Starter Mail
- WebSocket para notificações real-time
- Firebase Cloud Messaging (FCM)
- Thymeleaf para templates de email

### **🚀 9.3. EVOLUÇÃO FUTURA: IA para Otimização**

**Proposta Avançada:**
- **Machine Learning** para prever padrões de ocupação
- **Sugestão automática** de melhores horários/vagas
- **Precificação dinâmica** baseada em demanda
- **Análise preditiva** de comportamento dos usuários

**Tecnologias Sugeridas:**
- Spring AI Framework
- TensorFlow/PyTorch integration
- Apache Kafka para streaming de dados
- Redis para cache de predições

---

## 📊 **10. MÉTRICAS DE MELHORIA ALCANÇADAS**

| **Métrica** | **Valor Anterior** | **Valor Atual** |
|-------------|-------------------|-----------------|
| **Endpoints API** | 4 básicos | 15+ endpoints |
| **Tipos de Tarifa** | 1 tipo | 4 tipos |
| **Entidades JPA** | 2 entidades | 5 entidades |
| **Services** | 2 services | 6 services |
| **Funcionalidades** | CRUD básico | Sistema enterprise |
| **Relatórios** | 0 relatórios | 4 tipos |
| **Automação** | 0 jobs | 1 job scheduled |
| **Flexibilidade** | Baixa | Alta |

---

## 🔧 **11. INSTRUÇÕES DE INSTALAÇÃO E EXECUÇÃO**

### **11.1. Pré-requisitos**
- ✅ Java 17+
- ✅ Maven 3.8+
- ✅ Oracle Database 11g+
- ✅ Acesso ao oracle.fiap.com.br

### **11.2. Configuração do Banco**

**Passo 1:** Execute os scripts na ordem:
```sql
-- 1. Estrutura básica (já executado)
@db/oracle/01_schema.sql

-- 2. Dados de exemplo (já executado)  
@db/oracle/02_sample_data.sql

-- 3. NOVO: Melhorias implementadas
@db/oracle/03_melhorias.sql
```

**Passo 2:** Configure as variáveis de ambiente:
```bash
set ORACLE_URL=jdbc:oracle:thin:@oracle.fiap.com.br:1521:orcl
set ORACLE_USER=rm98047
set ORACLE_PASSWORD=201104
set PARKING_HOURLY_RATE=8.00
```

### **11.3. Execução da Aplicação**

```bash
# Compilar
mvn clean compile

# Executar
mvn spring-boot:run -Dspring-boot.run.profiles=sid

# Acessar Swagger UI
http://localhost:8080/swagger-ui.html

# Acessar API
http://localhost:8080/api
```

---

## 🧪 **12. TESTES E VALIDAÇÕES**

### **12.1. Testes Unitários Corrigidos**
- ✅ **TicketServiceTest** refatorado para novo construtor
- ✅ **Compilação** sem erros ou warnings
- ✅ **Execução** da aplicação bem-sucedida

### **12.2. Cenários de Teste Sugeridos**

**Teste de Reservas:**
1. Criar reserva válida
2. Tentar criar reserva com conflito de horário
3. Cancelar reserva ativa
4. Verificar expiração automática

**Teste de Tarifas:**
1. Check-in com tarifa fracionada (30min)
2. Check-in com tarifa horária
3. Check-in com tarifa diária
4. Comparar cálculos entre tipos

**Teste de Lotação:**
1. Simular estacionamento quase lotado
2. Tentar check-in quando lotado
3. Verificar métricas de ocupação

---

## 🏆 **13. RESULTADOS E CONCLUSÃO**

### **13.1. Objetivos Alcançados**

✅ **Refatoração completa** do código original  
✅ **Sistema de reservas** implementado e funcional  
✅ **Controle de lotação** inteligente  
✅ **4 tipos de tarifas** diferenciadas  
✅ **Sistema de relatórios** gerenciais  
✅ **Arquitetura escalável** e maintível  
✅ **Jobs automáticos** para limpeza  
✅ **15+ endpoints** RESTful  

### **13.2. Impacto no Negócio**

- 📈 **Aumento da eficiência** operacional
- 💰 **Maximização da receita** com tarifas flexíveis  
- 👥 **Melhoria na experiência** do usuário
- 📊 **Visibilidade gerencial** com relatórios
- 🔄 **Automação** de processos manuais
- 📱 **Preparação para mobile** com APIs RESTful

### **13.3. Evolução Arquitetural**

O projeto evoluiu de um **sistema básico** para uma **solução enterprise completa**, seguindo:

- ✅ **Princípios SOLID**
- ✅ **Clean Architecture**  
- ✅ **Domain-Driven Design (DDD)**
- ✅ **RESTful API Design**
- ✅ **Separation of Concerns**

---

## 📚 **14. TECNOLOGIAS E FRAMEWORKS UTILIZADOS**

| **Categoria** | **Tecnologia** | **Versão** | **Uso** |
|---------------|----------------|------------|---------|
| **Framework** | Spring Boot | 3.3.2 | Base da aplicação |
| **Persistência** | Spring Data JPA | 3.3.2 | Mapeamento objeto-relacional |
| **Banco de Dados** | Oracle Database | 11g+ | Persistência de dados |
| **Documentação** | OpenAPI/Swagger | 3.0 | Documentação da API |
| **Build** | Maven | 3.8+ | Gerenciamento de dependências |
| **Language** | Java | 17+ | Linguagem principal |
| **Agendamento** | Spring Scheduling | 6.1.11 | Jobs automáticos |
| **Validation** | Bean Validation | 2.0 | Validação de dados |


---

## 📄 **16. ANEXOS**

### **16.1. Estrutura Final do Projeto**
```
estacionamento-soa-springboot/
├── pom.xml
├── README.md
├── cp1.md (este documento)
├── db/oracle/
│   ├── 01_schema.sql
│   ├── 02_sample_data.sql  
│   └── 03_melhorias.sql (novo)
├── collections/
│   ├── API_Estacionamento.postman_collection.json
│   └── Insomnia_Estacionamento.json
└── src/main/java/br/edu/fiap/soa/estacionamento/
    ├── EstacionamentoApplication.java
    ├── domain/ (5 entidades)
    ├── repository/ (4 repositories)
    ├── service/ (6 services)
    ├── web/ (4 controllers + 8 DTOs)
    └── resources/application.properties
```


