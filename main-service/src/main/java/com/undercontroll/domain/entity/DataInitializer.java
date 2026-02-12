package com.undercontroll.domain.entity;

import com.undercontroll.domain.entity.enums.AnnouncementType;
import com.undercontroll.domain.entity.enums.OrderStatus;
import com.undercontroll.domain.entity.enums.UserType;
import com.undercontroll.infrastructure.persistence.repository.AnnouncementRepository;
import com.undercontroll.infrastructure.persistence.repository.ComponentJpaRepository;
import com.undercontroll.infrastructure.persistence.repository.DemandRepository;
import com.undercontroll.infrastructure.persistence.repository.OrderJpaRepository;
import com.undercontroll.infrastructure.persistence.repository.UserJpaRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class DataInitializer {

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private ComponentJpaRepository componentRepository;

    @Autowired
    private DemandRepository demandRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private PasswordEncoder encoder;

    @PostConstruct
    public void init() throws ParseException {
        // ====================================
        // USUÁRIOS
        // ====================================
        // Senha padrão para todos os usuários: "123"
        String defaultPassword = encoder.encode("123");

        User user1 = userRepository.save(User.builder()
                .name("Admin")
                .lastName("Sistema")
                .email("admin@undercontroll.com")
                .password(defaultPassword)
                .address("Rua Principal, 100")
                .cpf("11111111111")
                .CEP("01310100")
                .phone("11987654321")
                .alreadyRecurrent(true)
                .inFirstLogin(false)
                .avatarUrl("https://i.pravatar.cc/150?img=1")
                .hasWhatsApp(true)
                .userType(UserType.ADMINISTRATOR)
                .build());

        User user2 = userRepository.save(User.builder()
                .name("João")
                .lastName("Silva")
                .email("joao.silva@email.com")
                .password(defaultPassword)
                .address("Av. Paulista, 1000")
                .cpf("22222222222")
                .CEP("01310200")
                .phone("11987654322")
                .alreadyRecurrent(true)
                .inFirstLogin(false)
                .avatarUrl("https://i.pravatar.cc/150?img=12")
                .hasWhatsApp(true)
                .userType(UserType.CUSTOMER)
                .build());

        User user3 = userRepository.save(User.builder()
                .name("Maria")
                .lastName("Santos")
                .email("maria.santos@email.com")
                .password(defaultPassword)
                .address("Rua das Flores, 250")
                .cpf("33333333333")
                .CEP("01310300")
                .phone("11987654323")
                .alreadyRecurrent(true)
                .inFirstLogin(false)
                .avatarUrl("https://i.pravatar.cc/150?img=5")
                .hasWhatsApp(true)
                .userType(UserType.CUSTOMER)
                .build());

        User user4 = userRepository.save(User.builder()
                .name("Carlos")
                .lastName("Oliveira")
                .email("carlos.oliveira@email.com")
                .password(defaultPassword)
                .address("Rua do Comércio, 500")
                .cpf("44444444444")
                .CEP("01310400")
                .phone("11987654324")
                .alreadyRecurrent(false)
                .inFirstLogin(false)
                .avatarUrl("https://i.pravatar.cc/150?img=13")
                .hasWhatsApp(true)
                .userType(UserType.CUSTOMER)
                .build());

        User user5 = userRepository.save(User.builder()
                .name("Ana")
                .lastName("Costa")
                .email("ana.costa@email.com")
                .password(defaultPassword)
                .address("Av. Brasil, 750")
                .cpf("55555555555")
                .CEP("01310500")
                .phone("11987654325")
                .alreadyRecurrent(true)
                .inFirstLogin(false)
                .avatarUrl("https://i.pravatar.cc/150?img=9")
                .hasWhatsApp(true)
                .userType(UserType.CUSTOMER)
                .build());

        User user6 = userRepository.save(User.builder()
                .name("Pedro")
                .lastName("Almeida")
                .email("pedro.almeida@email.com")
                .password(defaultPassword)
                .address("Rua da Liberdade, 320")
                .cpf("66666666666")
                .CEP("01310600")
                .phone("11987654326")
                .alreadyRecurrent(false)
                .inFirstLogin(true)
                .avatarUrl("https://i.pravatar.cc/150?img=14")
                .hasWhatsApp(false)
                .userType(UserType.CUSTOMER)
                .build());

        User user7 = userRepository.save(User.builder()
                .name("Juliana")
                .lastName("Ferreira")
                .email("juliana.ferreira@email.com")
                .password(defaultPassword)
                .address("Rua dos Pinheiros, 180")
                .cpf("77777777777")
                .CEP("01310700")
                .phone("11987654327")
                .alreadyRecurrent(true)
                .inFirstLogin(false)
                .avatarUrl("https://i.pravatar.cc/150?img=10")
                .hasWhatsApp(true)
                .userType(UserType.CUSTOMER)
                .build());

        User user8 = userRepository.save(User.builder()
                .name("Roberto")
                .lastName("Mendes")
                .email("roberto.mendes@email.com")
                .password(defaultPassword)
                .address("Av. Faria Lima, 2000")
                .cpf("88888888888")
                .CEP("01310800")
                .phone("11987654328")
                .alreadyRecurrent(false)
                .inFirstLogin(false)
                .avatarUrl("https://i.pravatar.cc/150?img=15")
                .hasWhatsApp(true)
                .userType(UserType.CUSTOMER)
                .build());

        // ====================================
        // COMPONENTES / PEÇAS
        // ====================================
        ComponentPart component1 = componentRepository.save(ComponentPart.builder()
                .name("Resistor 10W")
                .description("Resistor de alta potência 10W")
                .brand("Vishay")
                .price(2.50)
                .supplier("Eletroparts Ltda")
                .category("Resistores")
                .quantity(150L)
                .build());

        ComponentPart component2 = componentRepository.save(ComponentPart.builder()
                .name("Capacitor 100uF")
                .description("Capacitor eletrolítico 100uF 25V")
                .brand("Nichicon")
                .price(1.80)
                .supplier("Eletroparts Ltda")
                .category("Capacitores")
                .quantity(200L)
                .build());

        ComponentPart component3 = componentRepository.save(ComponentPart.builder()
                .name("Transistor TIP122")
                .description("Transistor de potência NPN")
                .brand("Fairchild")
                .price(3.20)
                .supplier("Semicond Brasil")
                .category("Transistores")
                .quantity(80L)
                .build());

        ComponentPart component4 = componentRepository.save(ComponentPart.builder()
                .name("Diodo 1N4007")
                .description("Diodo retificador 1A 1000V")
                .brand("ON Semiconductor")
                .price(0.50)
                .supplier("Semicond Brasil")
                .category("Diodos")
                .quantity(300L)
                .build());

        ComponentPart component5 = componentRepository.save(ComponentPart.builder()
                .name("CI LM358")
                .description("Amplificador operacional duplo")
                .brand("Texas Instruments")
                .price(2.80)
                .supplier("Eletroparts Ltda")
                .category("Circuitos Integrados")
                .quantity(120L)
                .build());

        ComponentPart component6 = componentRepository.save(ComponentPart.builder()
                .name("Transformador 12V 2A")
                .description("Transformador 110/220V para 12V 2A")
                .brand("Hayonik")
                .price(25.00)
                .supplier("Transformadores SP")
                .category("Transformadores")
                .quantity(45L)
                .build());

        ComponentPart component7 = componentRepository.save(ComponentPart.builder()
                .name("Fusível 5A")
                .description("Fusível de vidro 5A 250V")
                .brand("Littel Fuse")
                .price(0.80)
                .supplier("Proteção Elétrica")
                .category("Fusíveis")
                .quantity(500L)
                .build());

        ComponentPart component8 = componentRepository.save(ComponentPart.builder()
                .name("Relé 12V 10A")
                .description("Relé eletromecânico 12V 10A")
                .brand("Songle")
                .price(4.50)
                .supplier("Eletroparts Ltda")
                .category("Relés")
                .quantity(90L)
                .build());

        ComponentPart component9 = componentRepository.save(ComponentPart.builder()
                .name("LED 5mm Vermelho")
                .description("LED alto brilho vermelho 5mm")
                .brand("Kingbright")
                .price(0.30)
                .supplier("LED Brasil")
                .category("LEDs")
                .quantity(1000L)
                .build());

        ComponentPart component10 = componentRepository.save(ComponentPart.builder()
                .name("Potenciômetro 10K")
                .description("Potenciômetro linear 10K")
                .brand("Alpha")
                .price(1.50)
                .supplier("Eletroparts Ltda")
                .category("Potenciômetros")
                .quantity(180L)
                .build());

        ComponentPart component11 = componentRepository.save(ComponentPart.builder()
                .name("Placa de Circuito Universal")
                .description("Placa universal 10x15cm")
                .brand("Genérico")
                .price(5.00)
                .supplier("Eletroparts Ltda")
                .category("Placas")
                .quantity(75L)
                .build());

        ComponentPart component12 = componentRepository.save(ComponentPart.builder()
                .name("Cabo PP 2x1.5mm")
                .description("Cabo paralelo 2x1.5mm (metro)")
                .brand("Cobrecom")
                .price(3.50)
                .supplier("Cabos & Fios")
                .category("Cabos")
                .quantity(200L)
                .build());

        ComponentPart component13 = componentRepository.save(ComponentPart.builder()
                .name("Conector P4 Macho")
                .description("Conector P4 macho para áudio")
                .brand("Genérico")
                .price(2.00)
                .supplier("Eletroparts Ltda")
                .category("Conectores")
                .quantity(150L)
                .build());

        ComponentPart component14 = componentRepository.save(ComponentPart.builder()
                .name("Cooler 12V 80mm")
                .description("Cooler ventilador 12V 80x80mm")
                .brand("Evercool")
                .price(15.00)
                .supplier("Ventilação Tech")
                .category("Coolers")
                .quantity(60L)
                .build());

        ComponentPart component15 = componentRepository.save(ComponentPart.builder()
                .name("Pasta Térmica 3g")
                .description("Pasta térmica para processadores")
                .brand("Arctic Silver")
                .price(12.00)
                .supplier("Thermal Solutions")
                .category("Acessórios")
                .quantity(100L)
                .build());

        // ====================================
        // ANÚNCIOS
        // ====================================
        announcementRepository.save(Announcement.builder()
                .title("Natal 2024")
                .content("A UnderControll estará fechada nos dias 24, 25 e 26 de dezembro. Feliz Natal a todos!")
                .type(AnnouncementType.HOLIDAY)
                .build());

        announcementRepository.save(Announcement.builder()
                .title("Ano Novo 2025")
                .content("Funcionamento especial: 31/12 até 12h. Retornamos dia 02/01/2025. Feliz Ano Novo!")
                .type(AnnouncementType.HOLIDAY)
                .build());

        announcementRepository.save(Announcement.builder()
                .title("Carnaval 2025")
                .content("Não haverá atendimento de 01/03 a 05/03. Bom carnaval!")
                .type(AnnouncementType.HOLIDAY)
                .build());

        announcementRepository.save(Announcement.builder()
                .title("Manutenção Programada")
                .content("Sistema em manutenção no dia 15/03 das 02h às 06h.")
                .type(AnnouncementType.HOLIDAY)
                .build());

        // ====================================
        // PEDIDOS (ORDERS) e ITENS
        // ====================================
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Order 1 - COMPLETED
        OrderItem orderItem1 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1598327105666-5b89351aff97")
                .observation("Fonte queimada, necessário troca de capacitores")
                .volt("110V")
                .series("PS-500W")
                .type("Fonte ATX")
                .brand("Corsair")
                .model("CX500")
                .laborValue(150.00)
                .completedAt(LocalDateTime.parse("2025-10-08T16:00:00"))
                .build();

        Order order1 = orderRepository.save(Order.builder()
                .user(user2)
                .orderItems(new ArrayList<>(List.of(orderItem1)))
                .demands(new ArrayList<>())
                .status(OrderStatus.COMPLETED)
                .total(250.00)
                .discount(0.00)
                .fabricGuarantee(true)
                .returnGuarantee(true)
                .description("Reparo em fonte de alimentação")
                .nf("NF-001")
                .date(sdf.parse("2025-01-05"))
                .store("Loja Centro")
                .received_at(LocalDate.parse("2025-01-05", dtf))
                .completedTime(LocalDate.parse("2025-01-12", dtf))
                .build());

        // Order 2 - COMPLETED
        OrderItem orderItem2 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1545128485-c400e7702796")
                .observation("Amplificador com ruído no canal esquerdo")
                .volt("220V")
                .series("AMP-100")
                .type("Amplificador")
                .brand("Onkyo")
                .model("TX-8020")
                .laborValue(120.00)
                .completedAt(LocalDateTime.parse("2025-10-10T14:30:00"))
                .build();

        Order order2 = orderRepository.save(Order.builder()
                .user(user3)
                .orderItems(new ArrayList<>(List.of(orderItem2)))
                .demands(new ArrayList<>())
                .status(OrderStatus.COMPLETED)
                .total(180.50)
                .discount(10.00)
                .fabricGuarantee(false)
                .returnGuarantee(true)
                .description("Manutenção em amplificador")
                .nf("NF-002")
                .date(sdf.parse("2025-01-15"))
                .store("Loja Centro")
                .received_at(LocalDate.parse("2025-01-15", dtf))
                .completedTime(LocalDate.parse("2025-01-20", dtf))
                .build());

        // Order 3 - COMPLETED (2 items)
        OrderItem orderItem3a = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1593359677879-a4bb92f829d1")
                .observation("TV com listras verticais na tela")
                .volt("110V")
                .series("LED-55")
                .type("TV LED")
                .brand("Samsung")
                .model("UN55TU7000")
                .laborValue(280.00)
                .completedAt(LocalDateTime.parse("2025-10-20T11:20:00"))
                .build();

        OrderItem orderItem3b = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1593359677879-a4bb92f829d1")
                .observation("Placa T-CON com defeito")
                .volt("110V")
                .series("LED-55")
                .type("TV LED")
                .brand("Samsung")
                .model("UN55TU7000")
                .laborValue(120.00)
                .completedAt(LocalDateTime.parse("2025-10-20T11:20:00"))
                .build();

        Order order3 = orderRepository.save(Order.builder()
                .user(user4)
                .orderItems(new ArrayList<>(List.of(orderItem3a, orderItem3b)))
                .demands(new ArrayList<>())
                .status(OrderStatus.COMPLETED)
                .total(420.00)
                .discount(20.00)
                .fabricGuarantee(true)
                .returnGuarantee(true)
                .description("Troca de componentes TV LED")
                .nf("NF-003")
                .date(sdf.parse("2025-02-01"))
                .store("Loja Centro")
                .received_at(LocalDate.parse("2025-02-01", dtf))
                .completedTime(LocalDate.parse("2025-02-10", dtf))
                .build());

        // Order 4 - COMPLETED
        OrderItem orderItem4 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1558618666-fcd25c85cd64")
                .observation("Placa de som sem áudio na saída frontal")
                .volt("5V")
                .series("SB-X")
                .type("Placa de Som")
                .brand("Creative")
                .model("Sound Blaster X3")
                .laborValue(60.00)
                .completedAt(LocalDateTime.parse("2025-02-25T15:45:00"))
                .build();

        Order order4 = orderRepository.save(Order.builder()
                .user(user5)
                .orderItems(new ArrayList<>(List.of(orderItem4)))
                .demands(new ArrayList<>())
                .status(OrderStatus.COMPLETED)
                .total(95.00)
                .discount(5.00)
                .fabricGuarantee(false)
                .returnGuarantee(false)
                .description("Reparo placa de som")
                .nf("NF-004")
                .date(sdf.parse("2024-10-15"))
                .store("Loja Norte")
                .received_at(LocalDate.parse("2024-10-15", dtf))
                .completedTime(LocalDate.parse("2024-10-18", dtf))
                .build());

        // Order 5 - COMPLETED
        OrderItem orderItem5 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1588872657578-7efd1f1555ed")
                .observation("Fonte do notebook não carrega bateria")
                .volt("19V")
                .series("NB-2021")
                .type("Notebook")
                .brand("Dell")
                .model("Inspiron 15 3000")
                .laborValue(180.00)
                .completedAt(LocalDateTime.parse("2025-03-17T10:00:00"))
                .build();

        Order order5 = orderRepository.save(Order.builder()
                .user(user7)
                .orderItems(new ArrayList<>(List.of(orderItem5)))
                .demands(new ArrayList<>())
                .status(OrderStatus.COMPLETED)
                .total(310.00)
                .discount(0.00)
                .fabricGuarantee(true)
                .returnGuarantee(true)
                .description("Conserto de notebook - fonte")
                .nf("NF-005")
                .date(sdf.parse("2024-11-01"))
                .store("Loja Centro")
                .received_at(LocalDate.parse("2024-11-01", dtf))
                .completedTime(LocalDate.parse("2024-11-07", dtf))
                .build());

        // Order 6 - COMPLETED
        OrderItem orderItem6 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1587202372634-32705e3bf49c")
                .observation("Cooler barulhento e superaquecimento")
                .volt("12V")
                .series("PC-2020")
                .type("Desktop")
                .brand("Custom Build")
                .model("Gaming PC")
                .laborValue(95.00)
                .completedAt(LocalDateTime.parse("2025-04-08T16:30:00"))
                .build();

        Order order6 = orderRepository.save(Order.builder()
                .user(user2)
                .orderItems(new ArrayList<>(List.of(orderItem6)))
                .demands(new ArrayList<>())
                .status(OrderStatus.COMPLETED)
                .total(155.00)
                .discount(0.00)
                .fabricGuarantee(false)
                .returnGuarantee(true)
                .description("Troca de cooler e limpeza")
                .nf("NF-006")
                .date(sdf.parse("2024-11-10"))
                .store("Loja Norte")
                .received_at(LocalDate.parse("2024-11-10", dtf))
                .completedTime(LocalDate.parse("2024-11-12", dtf))
                .build());

        // Order 7 - COMPLETED
        OrderItem orderItem7 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea")
                .observation("Motherboard não reconhece RAM")
                .volt("12V")
                .series("MB-X570")
                .type("Motherboard")
                .brand("ASUS")
                .model("ROG Strix X570")
                .laborValue(180.00)
                .completedAt(LocalDateTime.parse("2025-05-10T14:00:00"))
                .build();

        Order order7 = orderRepository.save(Order.builder()
                .user(user3)
                .orderItems(new ArrayList<>(List.of(orderItem7)))
                .demands(new ArrayList<>())
                .status(OrderStatus.COMPLETED)
                .total(275.00)
                .discount(15.00)
                .fabricGuarantee(true)
                .returnGuarantee(false)
                .description("Reparo motherboard")
                .nf("NF-007")
                .date(sdf.parse("2024-11-15"))
                .store("Loja Centro")
                .received_at(LocalDate.parse("2024-11-15", dtf))
                .completedTime(LocalDate.parse("2024-11-25", dtf))
                .build());

        // Order 8 - COMPLETED
        OrderItem orderItem8 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1587202372634-32705e3bf49c")
                .observation("Manutenção preventiva completa")
                .volt("110V")
                .series("PC-2019")
                .type("Desktop")
                .brand("HP")
                .model("Pavilion Desktop")
                .laborValue(120.00)
                .completedAt(LocalDateTime.parse("2025-06-18T09:30:00"))
                .build();

        Order order8 = orderRepository.save(Order.builder()
                .user(user5)
                .orderItems(new ArrayList<>(List.of(orderItem8)))
                .demands(new ArrayList<>())
                .status(OrderStatus.COMPLETED)
                .total(199.99)
                .discount(0.00)
                .fabricGuarantee(true)
                .returnGuarantee(true)
                .description("Manutenção preventiva PC")
                .nf("NF-008")
                .date(sdf.parse("2024-11-20"))
                .store("Loja Norte")
                .received_at(LocalDate.parse("2024-11-20", dtf))
                .completedTime(LocalDate.parse("2025-06-18", dtf))
                .build());

        // Order 15 - COMPLETED
        OrderItem orderItem15 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1593359677879-a4bb92f829d1")
                .observation("TV não liga, possível problema na placa de fonte")
                .volt("110V")
                .series("LED-50")
                .type("TV LED")
                .brand("LG")
                .model("50UN7310")
                .laborValue(200.00)
                .completedAt(LocalDateTime.parse("2025-07-10T14:00:00"))
                .build();

        Order order15 = orderRepository.save(Order.builder()
                .user(user4)
                .orderItems(new ArrayList<>(List.of(orderItem15)))
                .demands(new ArrayList<>())
                .status(OrderStatus.COMPLETED)
                .total(350.00)
                .discount(0.00)
                .fabricGuarantee(true)
                .returnGuarantee(true)
                .description("Reparo TV LED - placa fonte")
                .nf("NF-010")
                .date(sdf.parse("2025-07-01"))
                .store("Loja Centro")
                .received_at(LocalDate.parse("2025-07-01", dtf))
                .completedTime(LocalDate.parse("2025-07-10", dtf))
                .build());

        // Order 16 - COMPLETED
        OrderItem orderItem16 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1588872657578-7efd1f1555ed")
                .observation("Notebook com tela piscando")
                .volt("19V")
                .series("NB-2023")
                .type("Notebook")
                .brand("Lenovo")
                .model("IdeaPad 3")
                .laborValue(220.00)
                .completedAt(LocalDateTime.parse("2025-07-25T11:30:00"))
                .build();

        Order order16 = orderRepository.save(Order.builder()
                .user(user5)
                .orderItems(new ArrayList<>(List.of(orderItem16)))
                .demands(new ArrayList<>())
                .status(OrderStatus.COMPLETED)
                .total(320.00)
                .discount(10.00)
                .fabricGuarantee(true)
                .returnGuarantee(true)
                .description("Reparo notebook - tela")
                .nf("NF-011")
                .date(sdf.parse("2025-07-18"))
                .store("Loja Norte")
                .received_at(LocalDate.parse("2025-07-18", dtf))
                .completedTime(LocalDate.parse("2025-07-25", dtf))
                .build());

        // Order 17 - COMPLETED
        OrderItem orderItem17 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1587202372634-32705e3bf49c")
                .observation("PC com problema de inicialização")
                .volt("110V")
                .series("PC-2022")
                .type("Desktop")
                .brand("Dell")
                .model("OptiPlex 3080")
                .laborValue(150.00)
                .completedAt(LocalDateTime.parse("2025-08-08T15:00:00"))
                .build();

        Order order17 = orderRepository.save(Order.builder()
                .user(user7)
                .orderItems(new ArrayList<>(List.of(orderItem17)))
                .demands(new ArrayList<>())
                .status(OrderStatus.COMPLETED)
                .total(230.00)
                .discount(0.00)
                .fabricGuarantee(false)
                .returnGuarantee(true)
                .description("Reparo desktop - inicialização")
                .nf("NF-012")
                .date(sdf.parse("2025-08-01"))
                .store("Loja Centro")
                .received_at(LocalDate.parse("2025-08-01", dtf))
                .completedTime(LocalDate.parse("2025-08-08", dtf))
                .build());

        // Order 18 - COMPLETED
        OrderItem orderItem18 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1545128485-c400e7702796")
                .observation("Receiver sem saída de áudio")
                .volt("220V")
                .series("RCV-200")
                .type("Receiver")
                .brand("Sony")
                .model("STR-DH590")
                .laborValue(180.00)
                .completedAt(LocalDateTime.parse("2025-08-22T10:30:00"))
                .build();

        Order order18 = orderRepository.save(Order.builder()
                .user(user2)
                .orderItems(new ArrayList<>(List.of(orderItem18)))
                .demands(new ArrayList<>())
                .status(OrderStatus.COMPLETED)
                .total(280.00)
                .discount(15.00)
                .fabricGuarantee(true)
                .returnGuarantee(false)
                .description("Reparo receiver - saída áudio")
                .nf("NF-013")
                .date(sdf.parse("2025-08-15"))
                .store("Loja Norte")
                .received_at(LocalDate.parse("2025-08-15", dtf))
                .completedTime(LocalDate.parse("2025-08-22", dtf))
                .build());

        // Order 19 - COMPLETED
        OrderItem orderItem19 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea")
                .observation("Placa mãe com slots USB queimados")
                .volt("12V")
                .series("MB-B550")
                .type("Motherboard")
                .brand("MSI")
                .model("B550 GAMING PLUS")
                .laborValue(160.00)
                .completedAt(LocalDateTime.parse("2025-09-05T14:45:00"))
                .build();

        Order order19 = orderRepository.save(Order.builder()
                .user(user3)
                .orderItems(new ArrayList<>(List.of(orderItem19)))
                .demands(new ArrayList<>())
                .status(OrderStatus.COMPLETED)
                .total(240.00)
                .discount(0.00)
                .fabricGuarantee(true)
                .returnGuarantee(true)
                .description("Reparo motherboard - USB")
                .nf("NF-014")
                .date(sdf.parse("2025-09-01"))
                .store("Loja Centro")
                .received_at(LocalDate.parse("2025-09-01", dtf))
                .completedTime(LocalDate.parse("2025-09-05", dtf))
                .build());

        // Order 20 - COMPLETED
        OrderItem orderItem20 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1598327105666-5b89351aff97")
                .observation("Fonte com ruído e instabilidade")
                .volt("220V")
                .series("PS-750W")
                .type("Fonte ATX")
                .brand("Seasonic")
                .model("FOCUS GX-750")
                .laborValue(130.00)
                .completedAt(LocalDateTime.parse("2025-09-18T16:00:00"))
                .build();

        Order order20 = orderRepository.save(Order.builder()
                .user(user8)
                .orderItems(new ArrayList<>(List.of(orderItem20)))
                .demands(new ArrayList<>())
                .status(OrderStatus.COMPLETED)
                .total(210.00)
                .discount(5.00)
                .fabricGuarantee(false)
                .returnGuarantee(true)
                .description("Reparo fonte ATX - estabilização")
                .nf("NF-015")
                .date(sdf.parse("2025-09-12"))
                .store("Loja Norte")
                .received_at(LocalDate.parse("2025-09-12", dtf))
                .completedTime(LocalDate.parse("2025-09-18", dtf))
                .build());

        // Order 21 - COMPLETED
        OrderItem orderItem21 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1593359677879-a4bb92f829d1")
                .observation("TV com som mas sem imagem")
                .volt("110V")
                .series("LED-43")
                .type("TV LED")
                .brand("Philco")
                .model("PTV43G50SN")
                .laborValue(190.00)
                .completedAt(LocalDateTime.parse("2025-10-03T11:00:00"))
                .build();

        Order order21 = orderRepository.save(Order.builder()
                .user(user6)
                .orderItems(new ArrayList<>(List.of(orderItem21)))
                .demands(new ArrayList<>())
                .status(OrderStatus.COMPLETED)
                .total(290.00)
                .discount(0.00)
                .fabricGuarantee(true)
                .returnGuarantee(true)
                .description("Reparo TV LED - backlight")
                .nf("NF-016")
                .date(sdf.parse("2025-09-28"))
                .store("Loja Centro")
                .received_at(LocalDate.parse("2025-09-28", dtf))
                .completedTime(LocalDate.parse("2025-10-03", dtf))
                .build());

        // Order 22 - COMPLETED
        OrderItem orderItem22 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1588872657578-7efd1f1555ed")
                .observation("Notebook superaquecendo")
                .volt("19V")
                .series("NB-2024")
                .type("Notebook")
                .brand("ASUS")
                .model("VivoBook 15")
                .laborValue(140.00)
                .completedAt(LocalDateTime.parse("2025-10-15T13:30:00"))
                .build();

        Order order22 = orderRepository.save(Order.builder()
                .user(user4)
                .orderItems(new ArrayList<>(List.of(orderItem22)))
                .demands(new ArrayList<>())
                .status(OrderStatus.COMPLETED)
                .total(200.00)
                .discount(10.00)
                .fabricGuarantee(true)
                .returnGuarantee(false)
                .description("Manutenção notebook - limpeza térmica")
                .nf("NF-017")
                .date(sdf.parse("2025-10-10"))
                .store("Loja Norte")
                .received_at(LocalDate.parse("2025-10-10", dtf))
                .completedTime(LocalDate.parse("2025-10-15", dtf))
                .build());

        // Order 23 - COMPLETED
        OrderItem orderItem23 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1587202372634-32705e3bf49c")
                .observation("PC gamer com placa de vídeo com artefatos")
                .volt("110V")
                .series("PC-GAM-2023")
                .type("Desktop")
                .brand("Custom Build")
                .model("Gaming PC RTX")
                .laborValue(250.00)
                .completedAt(LocalDateTime.parse("2025-10-28T15:30:00"))
                .build();

        Order order23 = orderRepository.save(Order.builder()
                .user(user7)
                .orderItems(new ArrayList<>(List.of(orderItem23)))
                .demands(new ArrayList<>())
                .status(OrderStatus.COMPLETED)
                .total(380.00)
                .discount(20.00)
                .fabricGuarantee(false)
                .returnGuarantee(true)
                .description("Reparo GPU - artefatos")
                .nf("NF-018")
                .date(sdf.parse("2025-10-20"))
                .store("Loja Centro")
                .received_at(LocalDate.parse("2025-10-20", dtf))
                .completedTime(LocalDate.parse("2025-10-28", dtf))
                .build());

        // Order 24 - COMPLETED
        OrderItem orderItem24 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1527443224154-c4a3942d3acf")
                .observation("Monitor com linhas horizontais")
                .volt("110V")
                .series("MON-27")
                .type("Monitor IPS")
                .brand("Samsung")
                .model("S27R350")
                .laborValue(170.00)
                .completedAt(LocalDateTime.parse("2025-11-08T10:00:00"))
                .build();

        Order order24 = orderRepository.save(Order.builder()
                .user(user5)
                .orderItems(new ArrayList<>(List.of(orderItem24)))
                .demands(new ArrayList<>())
                .status(OrderStatus.COMPLETED)
                .total(260.00)
                .discount(0.00)
                .fabricGuarantee(true)
                .returnGuarantee(true)
                .description("Reparo monitor IPS - linhas")
                .nf("NF-019")
                .date(sdf.parse("2025-11-02"))
                .store("Loja Norte")
                .received_at(LocalDate.parse("2025-11-02", dtf))
                .completedTime(LocalDate.parse("2025-11-08", dtf))
                .build());

        // Order 25 - COMPLETED
        OrderItem orderItem25 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1612815154858-60aa4c59eaa6")
                .observation("Impressora com papel enroscando")
                .volt("110V")
                .series("PRINT-HP")
                .type("Impressora")
                .brand("HP")
                .model("LaserJet Pro M404")
                .laborValue(120.00)
                .completedAt(LocalDateTime.parse("2025-11-18T14:30:00"))
                .build();

        Order order25 = orderRepository.save(Order.builder()
                .user(user2)
                .orderItems(new ArrayList<>(List.of(orderItem25)))
                .demands(new ArrayList<>())
                .status(OrderStatus.COMPLETED)
                .total(180.00)
                .discount(5.00)
                .fabricGuarantee(false)
                .returnGuarantee(true)
                .description("Manutenção impressora - alimentação papel")
                .nf("NF-020")
                .date(sdf.parse("2025-11-12"))
                .store("Loja Centro")
                .received_at(LocalDate.parse("2025-11-12", dtf))
                .completedTime(LocalDate.parse("2025-11-18", dtf))
                .build());

        // Order 9 - IN_ANALYSIS
        OrderItem orderItem9 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1527443224154-c4a3942d3acf")
                .observation("Monitor com imagem tremida")
                .volt("110V")
                .series("MON-24")
                .type("Monitor LCD")
                .brand("LG")
                .model("24MP59G")
                .laborValue(150.00)
                .build();

        Order order9 = orderRepository.save(Order.builder()
                .user(user4)
                .orderItems(new ArrayList<>(List.of(orderItem9)))
                .demands(new ArrayList<>())
                .status(OrderStatus.IN_ANALYSIS)
                .total(280.00)
                .discount(0.00)
                .fabricGuarantee(true)
                .returnGuarantee(true)
                .description("Diagnóstico de monitor LCD")
                .date(sdf.parse("2025-11-25"))
                .store("Loja Centro")
                .received_at(LocalDate.parse("2025-11-25", dtf))
                .build());

        // Order 10 - IN_ANALYSIS
        OrderItem orderItem10 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea")
                .observation("PC não liga, possível problema na placa mãe")
                .volt("110V")
                .series("MB-B450")
                .type("Motherboard")
                .brand("Gigabyte")
                .model("B450 AORUS")
                .laborValue(200.00)
                .build();

        Order order10 = orderRepository.save(Order.builder()
                .user(user6)
                .orderItems(new ArrayList<>(List.of(orderItem10)))
                .demands(new ArrayList<>())
                .status(OrderStatus.IN_ANALYSIS)
                .total(350.00)
                .discount(0.00)
                .fabricGuarantee(false)
                .returnGuarantee(true)
                .description("Reparo placa mãe desktop")
                .date(sdf.parse("2025-11-28"))
                .store("Loja Norte")
                .received_at(LocalDate.parse("2025-11-28", dtf))
                .build());

        // Order 11 - IN_ANALYSIS
        OrderItem orderItem11 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1598327105666-5b89351aff97")
                .observation("Fonte com cheiro de queimado")
                .volt("220V")
                .series("PS-650W")
                .type("Fonte ATX")
                .brand("EVGA")
                .model("650W Bronze")
                .laborValue(100.00)
                .build();

        Order order11 = orderRepository.save(Order.builder()
                .user(user7)
                .orderItems(new ArrayList<>(List.of(orderItem11)))
                .demands(new ArrayList<>())
                .status(OrderStatus.IN_ANALYSIS)
                .total(175.00)
                .discount(0.00)
                .fabricGuarantee(true)
                .returnGuarantee(false)
                .description("Troca de capacitores fonte ATX")
                .date(sdf.parse("2025-12-01"))
                .store("Loja Centro")
                .received_at(LocalDate.parse("2025-12-01", dtf))
                .build());

        // Order 12 - PENDING
        OrderItem orderItem12 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1588872657578-7efd1f1555ed")
                .observation("Notebook Dell não dá sinal de vida")
                .volt("19V")
                .series("NB-2022")
                .type("Notebook")
                .brand("Dell")
                .model("Vostro 15")
                .laborValue(0.00)
                .build();

        Order order12 = orderRepository.save(Order.builder()
                .user(user8)
                .orderItems(new ArrayList<>(List.of(orderItem12)))
                .demands(new ArrayList<>())
                .status(OrderStatus.PENDING)
                .total(0.00)
                .discount(0.00)
                .fabricGuarantee(false)
                .returnGuarantee(false)
                .description("Notebook não liga - análise inicial")
                .date(sdf.parse("2025-12-05"))
                .store("Loja Norte")
                .received_at(LocalDate.parse("2025-12-05", dtf))
                .build());

        // Order 13 - PENDING
        OrderItem orderItem13 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1612815154858-60aa4c59eaa6")
                .observation("Impressora não imprime preto")
                .volt("110V")
                .series("PRINT-L3150")
                .type("Impressora")
                .brand("Epson")
                .model("L3150")
                .laborValue(0.00)
                .build();

        Order order13 = orderRepository.save(Order.builder()
                .user(user2)
                .orderItems(new ArrayList<>(List.of(orderItem13)))
                .demands(new ArrayList<>())
                .status(OrderStatus.PENDING)
                .total(0.00)
                .discount(0.00)
                .fabricGuarantee(true)
                .returnGuarantee(true)
                .description("Impressora com erro de impressão")
                .date(sdf.parse("2025-12-06"))
                .store("Loja Centro")
                .received_at(LocalDate.parse("2025-12-06", dtf))
                .build());

        // Order 14 - DELIVERED
        OrderItem orderItem14 = OrderItem.builder()
                .imageUrl("https://images.unsplash.com/photo-1625948515291-69613efd103f")
                .observation("Carregador não fornece energia")
                .volt("19V")
                .series("CHRG-DELL")
                .type("Carregador")
                .brand("Dell")
                .model("65W Original")
                .laborValue(130.00)
                .completedAt(LocalDateTime.parse("2025-11-05T10:00:00"))
                .build();

        Order order14 = orderRepository.save(Order.builder()
                .user(user3)
                .orderItems(new ArrayList<>(List.of(orderItem14)))
                .demands(new ArrayList<>())
                .status(OrderStatus.DELIVERED)
                .total(225.00)
                .discount(0.00)
                .fabricGuarantee(true)
                .returnGuarantee(true)
                .description("Conserto de carregador notebook")
                .nf("NF-009")
                .date(sdf.parse("2025-11-01"))
                .store("Loja Centro")
                .received_at(LocalDate.parse("2025-11-01", dtf))
                .completedTime(LocalDate.parse("2025-11-05", dtf))
                .build());

        // ====================================
        // DEMANDAS (DEMANDS)
        // ====================================
        // Order 1 - Fonte ATX
        demandRepository.save(Demand.builder().quantity(3L).component(component2).order(order1).build());
        demandRepository.save(Demand.builder().quantity(2L).component(component7).order(order1).build());

        // Order 2 - Amplificador
        demandRepository.save(Demand.builder().quantity(2L).component(component1).order(order2).build());
        demandRepository.save(Demand.builder().quantity(1L).component(component3).order(order2).build());
        demandRepository.save(Demand.builder().quantity(4L).component(component4).order(order2).build());

        // Order 3 - TV LED
        demandRepository.save(Demand.builder().quantity(1L).component(component5).order(order3).build());
        demandRepository.save(Demand.builder().quantity(2L).component(component2).order(order3).build());
        demandRepository.save(Demand.builder().quantity(1L).component(component8).order(order3).build());

        // Order 4 - Placa de som
        demandRepository.save(Demand.builder().quantity(1L).component(component10).order(order4).build());
        demandRepository.save(Demand.builder().quantity(2L).component(component13).order(order4).build());

        // Order 5 - Notebook fonte
        demandRepository.save(Demand.builder().quantity(1L).component(component6).order(order5).build());
        demandRepository.save(Demand.builder().quantity(2L).component(component2).order(order5).build());
        demandRepository.save(Demand.builder().quantity(1L).component(component4).order(order5).build());

        // Order 6 - Cooler e limpeza
        demandRepository.save(Demand.builder().quantity(1L).component(component14).order(order6).build());
        demandRepository.save(Demand.builder().quantity(1L).component(component15).order(order6).build());

        // Order 7 - Motherboard
        demandRepository.save(Demand.builder().quantity(3L).component(component2).order(order7).build());
        demandRepository.save(Demand.builder().quantity(2L).component(component8).order(order7).build());
        demandRepository.save(Demand.builder().quantity(1L).component(component5).order(order7).build());

        // Order 8 - Manutenção preventiva
        demandRepository.save(Demand.builder().quantity(1L).component(component14).order(order8).build());
        demandRepository.save(Demand.builder().quantity(2L).component(component15).order(order8).build());
        demandRepository.save(Demand.builder().quantity(1L).component(component12).order(order8).build());

        // Order 9 - Monitor LCD (IN_ANALYSIS)
        demandRepository.save(Demand.builder().quantity(4L).component(component2).order(order9).build());
        demandRepository.save(Demand.builder().quantity(1L).component(component6).order(order9).build());

        // Order 10 - Placa mãe (IN_ANALYSIS)
        demandRepository.save(Demand.builder().quantity(5L).component(component2).order(order10).build());
        demandRepository.save(Demand.builder().quantity(2L).component(component8).order(order10).build());
        demandRepository.save(Demand.builder().quantity(1L).component(component5).order(order10).build());

        // Order 11 - Fonte ATX (IN_ANALYSIS)
        demandRepository.save(Demand.builder().quantity(4L).component(component2).order(order11).build());
        demandRepository.save(Demand.builder().quantity(2L).component(component7).order(order11).build());
        demandRepository.save(Demand.builder().quantity(2L).component(component4).order(order11).build());

        // Order 14 - Carregador (DELIVERED)
        demandRepository.save(Demand.builder().quantity(1L).component(component6).order(order14).build());
        demandRepository.save(Demand.builder().quantity(2L).component(component4).order(order14).build());

        // Order 15 - TV LED placa fonte (COMPLETED)
        demandRepository.save(Demand.builder().quantity(4L).component(component2).order(order15).build());
        demandRepository.save(Demand.builder().quantity(2L).component(component4).order(order15).build());
        demandRepository.save(Demand.builder().quantity(1L).component(component8).order(order15).build());

        // Order 16 - Notebook tela (COMPLETED)
        demandRepository.save(Demand.builder().quantity(2L).component(component12).order(order16).build());
        demandRepository.save(Demand.builder().quantity(1L).component(component13).order(order16).build());

        // Order 17 - Desktop inicialização (COMPLETED)
        demandRepository.save(Demand.builder().quantity(3L).component(component2).order(order17).build());
        demandRepository.save(Demand.builder().quantity(1L).component(component5).order(order17).build());
        demandRepository.save(Demand.builder().quantity(2L).component(component7).order(order17).build());

        // Order 18 - Receiver áudio (COMPLETED)
        demandRepository.save(Demand.builder().quantity(2L).component(component1).order(order18).build());
        demandRepository.save(Demand.builder().quantity(3L).component(component2).order(order18).build());
        demandRepository.save(Demand.builder().quantity(1L).component(component3).order(order18).build());

        // Order 19 - Motherboard USB (COMPLETED)
        demandRepository.save(Demand.builder().quantity(2L).component(component5).order(order19).build());
        demandRepository.save(Demand.builder().quantity(3L).component(component2).order(order19).build());

        // Order 20 - Fonte ATX estabilização (COMPLETED)
        demandRepository.save(Demand.builder().quantity(4L).component(component2).order(order20).build());
        demandRepository.save(Demand.builder().quantity(2L).component(component7).order(order20).build());
        demandRepository.save(Demand.builder().quantity(1L).component(component4).order(order20).build());

        // Order 21 - TV LED backlight (COMPLETED)
        demandRepository.save(Demand.builder().quantity(2L).component(component2).order(order21).build());
        demandRepository.save(Demand.builder().quantity(1L).component(component8).order(order21).build());
        demandRepository.save(Demand.builder().quantity(3L).component(component4).order(order21).build());

        // Order 22 - Notebook limpeza térmica (COMPLETED)
        demandRepository.save(Demand.builder().quantity(1L).component(component14).order(order22).build());
        demandRepository.save(Demand.builder().quantity(2L).component(component15).order(order22).build());

        // Order 23 - GPU artefatos (COMPLETED)
        demandRepository.save(Demand.builder().quantity(2L).component(component15).order(order23).build());
        demandRepository.save(Demand.builder().quantity(1L).component(component14).order(order23).build());
        demandRepository.save(Demand.builder().quantity(3L).component(component2).order(order23).build());

        // Order 24 - Monitor IPS linhas (COMPLETED)
        demandRepository.save(Demand.builder().quantity(3L).component(component2).order(order24).build());
        demandRepository.save(Demand.builder().quantity(1L).component(component5).order(order24).build());

        // Order 25 - Impressora alimentação papel (COMPLETED)
        demandRepository.save(Demand.builder().quantity(1L).component(component8).order(order25).build());
        demandRepository.save(Demand.builder().quantity(2L).component(component12).order(order25).build());
    }
}