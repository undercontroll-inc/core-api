package com.undercontroll.application.controller.publicpage;

import com.undercontroll.domain.enums.ApprovalDecision;
import com.undercontroll.domain.exception.TrackingNotFoundException;
import com.undercontroll.domain.model.ShopProfile;
import com.undercontroll.domain.usecase.order.DecidePublicOrderPort;
import com.undercontroll.domain.usecase.order.GetPublicOrderPort;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URI;
import java.time.Duration;

@Hidden
@Controller
@RequestMapping("/p")
public class PublicOrderController {

    static final String SECURITY_POLICY = "default-src 'none'; img-src 'self' %s; style-src 'unsafe-inline'; "
            + "form-action 'self'; base-uri 'none'; frame-ancestors 'none'";
    private static final Resource LOGO = new ClassPathResource("public-page/logo.png");

    private final GetPublicOrderPort getPublicOrderPort;
    private final DecidePublicOrderPort decidePublicOrderPort;
    private final ShopProfile shopProfile;
    private final String securityPolicy;

    public PublicOrderController(
            GetPublicOrderPort getPublicOrderPort,
            DecidePublicOrderPort decidePublicOrderPort,
            ShopProfile shopProfile,
            @Value("${undercontroll.storage.public-endpoint:}") String storagePublicEndpoint
    ) {
        this.getPublicOrderPort = getPublicOrderPort;
        this.decidePublicOrderPort = decidePublicOrderPort;
        this.shopProfile = shopProfile;
        this.securityPolicy = SECURITY_POLICY.formatted(imageSources(storagePublicEndpoint));
    }

    static String imageSources(String storagePublicEndpoint) {
        if (storagePublicEndpoint == null || storagePublicEndpoint.isBlank()) {
            return "https:";
        }
        URI uri = URI.create(storagePublicEndpoint.strip());
        String origin = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
        return origin;
    }

    @GetMapping("/{token}")
    public String page(
            @PathVariable String token,
            @RequestParam(name = "confirmar", required = false) String confirm,
            Model model,
            HttpServletResponse response
    ) {
        headers(response);
        model.addAttribute("page", PublicOrderPage.of(getPublicOrderPort.execute(token), confirm, shopProfile.zone()));
        model.addAttribute("shopName", shopProfile.name());
        return "public/order";
    }

    @PostMapping(value = "/{token}/decision", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public RedirectView decide(@PathVariable String token, @RequestParam(name = "decision", required = false) String decision) {
        decidePublicOrderPort.execute(token, parse(decision));
        RedirectView redirect = new RedirectView("/p/" + token, true);
        redirect.setStatusCode(HttpStatus.SEE_OTHER);
        return redirect;
    }

    @GetMapping("/assets/logo.png")
    public ResponseEntity<Resource> logo() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .contentType(MediaType.IMAGE_PNG)
                .body(LOGO);
    }

    @ExceptionHandler(TrackingNotFoundException.class)
    public ModelAndView notFound(HttpServletResponse response) {
        headers(response);
        ModelAndView view = new ModelAndView("public/not-found");
        view.addObject("shopName", shopProfile.name());
        view.setStatus(HttpStatus.NOT_FOUND);
        return view;
    }

    private static ApprovalDecision parse(String decision) {
        if ("APPROVED".equals(decision)) {
            return ApprovalDecision.APPROVED;
        }
        if ("DECLINED".equals(decision)) {
            return ApprovalDecision.DECLINED;
        }
        return null;
    }

    private void headers(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Content-Security-Policy", securityPolicy);
        response.setHeader("X-Robots-Tag", "noindex, nofollow");
        response.setHeader("Referrer-Policy", "no-referrer");
    }
}
