package com.undercontroll.infrastructure.ai;

import com.undercontroll.domain.gateway.AnnouncementGateway;
import com.undercontroll.domain.gateway.ComponentGateway;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.Announcement;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.Demand;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

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
    public OrderDetail getOrder(
            @ToolParam(description = "Id numérico do conserto/pedido") Integer orderId
    ) {
        try {
            return orderGateway.findDetailById(orderId)
                    .map(AnaShopTools::toOrderDetail)
                    .orElse(null);
        } catch (RuntimeException ex) {
            log.warn("Ana shop tool failed tool=get_order orderId={}", orderId, ex);
            return null;
        }
    }

    @Tool(name = "search_components", description = """
            Busca peças do estoque por nome ou categoria. Se ambos vazios, lista as de menor estoque.
            Use quando o dono perguntar de uma peça ou categoria que o resumo não cobre.
            O resumo já traz as peças com pouco estoque; não chame só para repetir isso.
            """)
    public List<PartLine> searchComponents(
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
            return parts.stream().limit(LIMIT).map(AnaShopTools::toPartLine).toList();
        } catch (RuntimeException ex) {
            log.warn("Ana shop tool failed tool=search_components", ex);
            return List.of();
        }
    }

    @Tool(name = "get_last_announcement", description = """
            Conteúdo completo do último aviso do aplicativo (título, texto e tipo).
            O resumo só tem o título. Use se pedirem o que o aviso diz.
            """)
    public AnnouncementLine getLastAnnouncement() {
        try {
            return announcementGateway.findLastAnnouncement()
                    .map(AnaShopTools::toAnnouncementLine)
                    .orElse(null);
        } catch (RuntimeException ex) {
            log.warn("Ana shop tool failed tool=get_last_announcement", ex);
            return null;
        }
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
