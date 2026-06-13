package com.example.ecommerce.product.adapter.outbound.search;

import com.example.ecommerce.product.domain.port.inbound.SearchProductUseCase.ProductView;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

@Document(indexName = "products")
class ProductDocument {

    @Id
    String id;

    @Field(type = FieldType.Keyword)
    String sku;

    @Field(type = FieldType.Text)
    String name;

    @Field(type = FieldType.Double)
    Double price;

    @Field(type = FieldType.Keyword)
    String currency;

    @Field(type = FieldType.Boolean)
    Boolean active;

    ProductView toView() {
        return new ProductView(
                id,
                sku,
                name,
                BigDecimal.valueOf(price),
                currency,
                Boolean.TRUE.equals(active));
    }
}
