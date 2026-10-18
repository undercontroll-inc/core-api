package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.gateway.AnnouncementGateway;
import com.undercontroll.domain.gateway.ComponentGateway;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.Announcement;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.Demand;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.model.PaginatedResult;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.support.Texts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnaShopTools {

    public static final int LIMIT = 15;

    private final OrderGateway orderGateway;
    private final ComponentGateway componentGateway;
    private final AnnouncementGateway announcementGateway;

    @Tool(name = "get_order", description = """
            Detalhe de um conserto/pedido pelo id, com itens, peças pedidas, total e loja.
            Use só se o resumo da oficina não tiver esse pedido, ou se o dono pedir um pedido específico.
            Não use para listar consertos em andamento — isso já está no resumo.
            """)
    public ToolResult<OrderDetail> getOrder(
            @ToolParam(description = "Id numérico do conserto/pedido") Integer orderId
    ) {
        if (orderId == null || orderId <= 0) {
            return ToolResult.invalidInput("Informe o número do pedido.");
        }
        try {
            return orderGateway.findDetailById(orderId)
                    .map(AnaShopTools::toOrderDetail)
                    .map(ToolResult::ok)
                    .orElseGet(() -> ToolResult.notFound("Pedido " + orderId + " não encontrado."));
        } catch (RuntimeException ex) {
            return unavailable("get_order", ex);
        }
    }

    @Tool(name = "list_orders", description = """
            Lista consertos/pedidos por situação, paginado (15 por página, mais recentes primeiro).
            Use quando o dono quiser todos os consertos de uma situação ou quando o resumo disser que há mais.
            Responde com total e has_more; se has_more for true, diga que há mais e ofereça a próxima página.
            """)
    public ToolResult<OrderPage> listOrders(
            @ToolParam(required = false, description = "abertos, prontos, entregues ou todos (padrão: abertos)") String status,
            @ToolParam(required = false, description = "Página, começando em 1") Integer page
    ) {
        List<OrderStatus> statuses = statuses(status);
        if (statuses == null) {
            return ToolResult.invalidInput("Situação inválida. Use abertos, prontos, entregues ou todos.");
        }
        int pageNumber = page == null || page < 1 ? 1 : page;
        try {
            PaginatedResult<Order> result = orderGateway.findPageByStatusIn(statuses, pageNumber - 1, LIMIT);
            if (result.totalElements() == 0) {
                return ToolResult.notFound("Nenhum conserto nessa situação.");
            }
            List<OrderLine> orders = result.content().stream().map(AnaShopTools::toOrderLine).toList();
            boolean hasMore = (long) pageNumber * LIMIT < result.totalElements();
            return ToolResult.ok(new OrderPage(orders, pageNumber, result.totalElements(), hasMore));
        } catch (RuntimeException ex) {
            return unavailable("list_orders", ex);
        }
    }

    static List<OrderStatus> statuses(String status) {
        String value = status == null || status.isBlank() ? "abertos" : Texts.stripAccents(status.trim().toLowerCase(Locale.ROOT));
        return switch (value) {
            case "abertos", "aberto", "em andamento", "andamento", "pending", "in_analysis" ->
                    List.of(OrderStatus.PENDING, OrderStatus.IN_ANALYSIS);
            case "prontos", "pronto", "completed" -> List.of(OrderStatus.COMPLETED);
            case "entregues", "entregue", "delivered" -> List.of(OrderStatus.DELIVERED);
            case "todos", "all" -> List.of(OrderStatus.values());
            default -> null;
        };
    }

    @Tool(name = "search_components", description = """
            Busca peças do estoque por nome ou categoria. Se ambos vazios, lista as de menor estoque.
            Use quando o dono perguntar de uma peça ou categoria que o resumo não cobre.
            O resumo já traz as peças com pouco estoque; não chame só para repetir isso.
            """)
    public ToolResult<List<PartLine>> searchComponents(
            @ToolParam(required = false, description = "Nome da peça; vazio se for buscar por categoria ou listar estoque baixo") String name,
            @ToolParam(required = false, description = "Categoria da peça; vazio se o nome já foi informado") String category
    ) {
        try {
            List<ComponentPart> parts;
            if (name != null && !name.isBlank()) {
                parts = componentGateway.searchByName(name, LIMIT);
            } else if (category != null && !category.isBlank()) {
                parts = componentGateway.searchByCategory(category, LIMIT);
            } else {
                parts = componentGateway.findLowestStock(LIMIT);
            }
            if (parts.isEmpty()) {
                return ToolResult.notFound("Nenhuma peça encontrada no estoque para essa busca.");
            }
            return ToolResult.ok(parts.stream().limit(LIMIT).map(AnaShopTools::toPartLine).toList());
        } catch (RuntimeException ex) {
            return unavailable("search_components", ex);
        }
    }

    @Tool(name = "get_last_announcement", description = """
            Conteúdo completo do último aviso do aplicativo (título, texto e tipo).
            O resumo só tem o título. Use se pedirem o que o aviso diz.
            """)
    public ToolResult<AnnouncementLine> getLastAnnouncement() {
        try {
            return announcementGateway.findLastAnnouncement()
                    .map(AnaShopTools::toAnnouncementLine)
                    .map(ToolResult::ok)
                    .orElseGet(() -> ToolResult.notFound("Nenhum aviso publicado."));
        } catch (RuntimeException ex) {
            return unavailable("get_last_announcement", ex);
        }
    }

    private static <T> ToolResult<T> unavailable(String tool, RuntimeException ex) {
        String traceId = ToolResult.newTraceId();
        log.warn("Ana shop tool failed tool={} traceId={}", tool, traceId, ex);
        return ToolResult.unavailable(traceId, true);
    }

    private static OrderLine toOrderLine(Order order) {
        User user = order.getUser();
        String customer = user == null ? null : user.fullName();
        int items = order.getOrderItems() == null ? 0 : order.getOrderItems().size();
        return new OrderLine(
                order.getId(),
                order.getStatus() == null ? null : order.getStatus().name(),
                customer,
                order.getCustomerDescription(),
                order.getTechnicalDescription(),
                items
        );
    }

    private static OrderDetail toOrderDetail(Order order) {
        OrderLine header = toOrderLine(order);
        List<ItemLine> items = order.getOrderItems() == null
                ? List.of()
                : order.getOrderItems().stream().limit(LIMIT).map(AnaShopTools::toItemLine).toList();
        List<DemandLine> demands = order.getDemands() == null
                ? List.of()
                : order.getDemands().stream().limit(LIMIT).map(AnaShopTools::toDemandLine).toList();
        return new OrderDetail(header, items, demands, order.getTotal(), order.getStore());
    }

    private static ItemLine toItemLine(OrderItem item) {
        return new ItemLine(
                item.getId(),
                item.getBrand(),
                item.getModel(),
                item.getType(),
                item.getLaborValue()
        );
    }

    private static PartLine toPartLine(ComponentPart part) {
        return new PartLine(
                part.getId(),
                part.getName(),
                part.getBrand(),
                part.getCategory(),
                part.getQuantity(),
                part.getPrice(),
                part.getSupplier()
        );
    }

    private static DemandLine toDemandLine(Demand demand) {
        ComponentPart part = demand.getComponent();
        Integer orderId = demand.getOrder() == null ? null : demand.getOrder().getId();
        return new DemandLine(
                demand.getId(),
                orderId,
                part == null ? null : part.getId(),
                part == null ? null : part.getName(),
                demand.getQuantity()
        );
    }

    private static AnnouncementLine toAnnouncementLine(Announcement announcement) {
        return new AnnouncementLine(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getType() == null ? null : announcement.getType().name()
        );
    }

    public record OrderLine(
            Integer id,
            String status,
            String customer,
            String customerDescription,
            String technicalDescription,
            int itemCount
    ) {
    }

    public record OrderPage(
            List<OrderLine> orders,
            int page,
            long total,
            @JsonProperty("has_more") boolean hasMore
    ) {
    }

    public record OrderDetail(OrderLine order, List<ItemLine> items, List<DemandLine> demands, Double total, String store) {
    }

    public record ItemLine(Integer id, String brand, String model, String type, Double laborValue) {
    }

    public record PartLine(
            Integer id,
            String name,
            String brand,
            String category,
            Long quantity,
            Double price,
            String supplier
    ) {
    }

    public record DemandLine(Integer id, Integer orderId, Integer componentId, String componentName, Long quantity) {
    }

    public record AnnouncementLine(Integer id, String title, String content, String type) {
    }
}
