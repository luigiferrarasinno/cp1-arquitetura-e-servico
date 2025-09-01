# 🅿️ API de Estacionamento — CP1 SOA Refatorado
**Professor:** Salatiel Luz Marinho • **Disciplina:** Arquitetura Orientada a Serviços  
**Aluno:** Luigi Ferrara • **RM:** rm98047

---

## 🎯 **OBJETIVO DO CP1**

Refatorar o sistema básico de estacionamento implementando:
1. ✅ **Refactor do código** com explicação dos motivos
2. ✅ **Reservas de vaga** e controle de lotação
3. ✅ **Tarifas diferenciadas** (diária, frações de 30min)
4. ✅ **3 evoluções identificadas** (1 implementada)

---

## 🔧 **REFACTORING APLICADO**

### **❌ PROBLEMAS IDENTIFICADOS**
- **Violação SRP**: `TicketService` criava veículos
- **Lógica limitada**: Apenas cálculo por hora
- **Sem controle de capacidade**: Não verificava lotação

### **✅ SOLUÇÕES IMPLEMENTADAS**
- **6 Services especializados**: Separação de responsabilidades
- **Sistema flexível de tarifas**: 4 tipos diferentes
- **Controle inteligente**: Validação antes do check-in

---

## 🆕 **FUNCIONALIDADES IMPLEMENTADAS**

### **📋 1. Sistema de Reservas** ✅
- Criação de reservas por período
- Validação de conflitos automática
- Expiração automática (job a cada 15min)
- Integração com check-in

### **🏢 2. Controle de Lotação** ✅
- Verificação antes do check-in
- Métricas em tempo real
- Status de lotação (ocupadas/livres)

### **💰 3. Tarifas Diferenciadas** ✅
- **FRACAO_30MIN**: Blocos de 30 minutos
- **HORARIA**: Por hora (original)
- **DIARIA**: Até 24h (compara com horária)
- **MENSAL**: Valor fixo

---

## 🚀 **MELHORIAS VISTAS - 3 EVOLUÇÕES IDENTIFICADAS**

### **✅ 1. SISTEMA DE RELATÓRIOS GERENCIAIS (IMPLEMENTADO)**

**O que foi feito:**
- 4 tipos de relatórios: Receita, Ocupação, Uso de Vagas, Consolidado
- Endpoints específicos: `/api/relatorios/*`
- Métricas em tempo real para gestão

**Por que é importante:**
- Visibilidade gerencial do negócio
- Controle de receita e ocupação
- Base para tomada de decisões

**Evidência da implementação:**
```bash
GET /api/relatorios/receita     # Receita total por período
GET /api/relatorios/ocupacao    # Status atual do estacionamento
GET /api/relatorios/vagas       # Uso das vagas individuais
GET /api/relatorios/consolidado # Visão 360° do negócio
```

### **🔄 2. SISTEMA DE NOTIFICAÇÕES (PRÓXIMA EVOLUÇÃO)**

**Proposta:**
- Notificações push/email para reservas expirando
- Alertas de lotação em tempo real
- Lembretes de check-out próximo ao vencimento

**Tecnologias sugeridas:**
- Spring Boot Starter Mail
- WebSocket para real-time
- Firebase Cloud Messaging (FCM)

**Benefício esperado:**
- Melhor experiência do usuário
- Redução de reservas perdidas
- Comunicação proativa

### **🚀 3. IA PARA OTIMIZAÇÃO (EVOLUÇÃO FUTURA)**

**Proposta:**
- Machine Learning para prever padrões de ocupação
- Precificação dinâmica baseada em demanda
- Sugestão automática de melhores horários

**Tecnologias sugeridas:**
- Spring AI Framework
- TensorFlow/PyTorch
- Apache Kafka para streaming
- Redis para cache de predições

**Benefício esperado:**
- Otimização automática de preços
- Máxima utilização das vagas
- Receita inteligente baseada em demanda

---

## 💰 **SISTEMA DE TARIFAS IMPLEMENTADO**

| **Tipo** | **Exemplo** | **Cálculo** |
|----------|-------------|-------------|
| **FRACAO_30MIN** | 2h30min = R$ 20,00 | 5 blocos × R$ 4,00 |
| **HORARIA** | 2h30min = R$ 24,00 | 3 horas × R$ 8,00 |
| **DIARIA** | 25h = R$ 38,00 | 1 dia + 1h extra |
| **MENSAL** | Qualquer tempo = R$ 200,00 | Valor fixo |

---

## 🌐 **PRINCIPAIS ENDPOINTS**

### **🎫 Tickets (Melhorados)**
```http
POST /api/tickets/checkin    # Check-in com tipo de tarifa
POST /api/tickets/{id}/checkout  # Check-out inteligente
```

### **📋 Reservas (Novo)**
```http
POST /api/reservas           # Criar reserva
GET  /api/reservas/ativas    # Listar reservas ativas
```

### **📊 Relatórios (Novo)**
```http
GET /api/relatorios/receita     # Relatório de receita
GET /api/relatorios/ocupacao    # Status do estacionamento
```

### **⚙️ Estacionamento (Novo)**
```http
GET /api/estacionamento/status  # Métricas em tempo real
```

---

## 📊 **RESULTADOS ALCANÇADOS**

| **Métrica** | **Antes** | **Depois** |
|-------------|-----------|------------|
| **Endpoints** | 4 básicos | 15+ endpoints |
| **Tarifas** | 1 tipo | 4 tipos |
| **Services** | 2 services | 6 services |
| **Relatórios** | 0 | 4 tipos |

---

## 🚀 **COMO EXECUTAR**

### **📋 Pré-requisitos**
- Java 17+, Maven 3.8+, Oracle Database

### **⚙️ Passos**

1. **Configure variáveis:**
   ```cmd
   set ORACLE_URL=jdbc:oracle:thin:@oracle.fiap.com.br:1521:orcl
   set ORACLE_USER=rm98047
   set ORACLE_PASSWORD=201104
   ```

2. **Execute scripts SQL (IMPORTANTE!):**
   ```cmd
   sqlplus rm98047/201104@oracle.fiap.com.br:1521/orcl
   @db/oracle/01_schema.sql
   @db/oracle/02_sample_data.sql
   @db/oracle/03_melhorias.sql
   ```

3. **Execute aplicação:**
   ```cmd
   mvn spring-boot:run -Dspring-boot.run.profiles=sid
   ```

4. **Acesse:**
   - Swagger: http://localhost:8080/swagger-ui.html
   - API: http://localhost:8080/api

---

## 📝 **EXEMPLOS DE TESTE**

### **Check-in com tarifa personalizada:**
```json
POST /api/tickets/checkin
{
  "placa": "ABC-1234",
  "modelo": "Honda Civic", 
  "vaga": "A10",
  "tipoTarifa": "FRACAO_30MIN"
}
```

### **Criar reserva:**
```json
POST /api/reservas
{
  "placa": "ABC-1234",
  "vaga": "A10",
  "dataInicio": "2025-09-02T09:00:00",
  "dataFim": "2025-09-02T18:00:00"  
}
```

### **Ver status do estacionamento:**
```json
GET /api/estacionamento/status
// Retorna: vagas ocupadas/livres, taxa ocupação
```

---

## 🏗️ **ARQUITETURA FINAL**

```
📁 Services Especializados:
├── TicketService (refatorado - foca só em tickets)
├── TarifaService (novo - cálculos de preço)  
├── ReservaService (novo - reservas)
├── EstacionamentoService (novo - controle lotação)
├── RelatorioService (novo - relatórios gerenciais)
└── VeiculoService (mantido - CRUD veículos)
```

**🗄️ Banco de Dados:**
- Tabelas originais: `veiculo`, `ticket`
- Novas tabelas: `estacionamento_config`, `reserva`
- Novas colunas: `ticket.tipo_tarifa`, `ticket.reserva_id`

---

## 📞 **DOCUMENTAÇÃO COMPLETA**

- 📖 **Detalhes técnicos**: Veja `cp1.md` (documentação de 700+ linhas)
- 🌐 **Teste na prática**: Swagger UI após executar
- 📧 **Suporte**: Professor Salatiel Luz Marinho

---

**🎓 CP1 CONCLUÍDO - Todas as exigências implementadas:**
✅ Refactor explicado • ✅ Reservas + Lotação • ✅ Tarifas diferenciadas • ✅ 3 evoluções (1 implementada)"# CP1-arquitetura-e-servico" 
