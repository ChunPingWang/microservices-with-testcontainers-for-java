package com.example.ecommerce.product.adapter.inbound.rest;

import com.example.ecommerce.product.domain.port.inbound.SearchProductUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final SearchProductUseCase searchProduct;

    public ProductController(SearchProductUseCase searchProduct) {
        this.searchProduct = searchProduct;
    }

    @GetMapping("/search")
    public List<SearchProductUseCase.ProductView> search(
            @RequestParam(name = "q", required = false, defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return searchProduct.search(new SearchProductUseCase.SearchQuery(keyword, page, size));
    }
}
