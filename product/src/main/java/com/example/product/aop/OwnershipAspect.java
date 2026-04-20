package com.example.product.aop;

import com.example.product.entity.Product;
import com.example.product.repository.ProductRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Aspect
@Component
public class OwnershipAspect {

    private final ProductRepository productRepository;
    private final JwtDecoder jwtDecoder;

    public OwnershipAspect(ProductRepository productRepository, JwtDecoder jwtDecoder) {
        this.productRepository = productRepository;
        this.jwtDecoder = jwtDecoder;
    }

    @Around("@annotation(OwnershipCheck)")
    public Object checkOwnership(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return joinPoint.proceed();

        HttpServletRequest request = attrs.getRequest();
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("Missing or invalid Authorization header");
        }

        Jwt jwt = jwtDecoder.decode(authHeader.substring(7));
        String username = jwt.getClaimAsString("username");
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) roles = List.of();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        Long productId = null;
        for (int i = 0; i < paramNames.length; i++) {
            if ("id".equals(paramNames[i]) && args[i] instanceof Long) {
                productId = (Long) args[i];
                break;
            }
        }

        if (productId == null) return joinPoint.proceed();

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) throw new RuntimeException("Product not found with id: " + productId);

        if (roles.contains("PRODUCT_ADMIN")) {
            return joinPoint.proceed();
        }

        if (product.getCreatedBy() != null && product.getCreatedBy().equals(username)) {
            return joinPoint.proceed();
        }

        throw new AccessDeniedException("无权限操作此产品，只有创建者或 PRODUCT_ADMIN 可删除");
    }
}
