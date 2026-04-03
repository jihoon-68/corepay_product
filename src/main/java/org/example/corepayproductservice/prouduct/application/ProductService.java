package org.example.corepayproductservice.prouduct.application;

import org.example.corepayproductservice.prouduct.presentation.dto.req.ProductCreatReq;
import org.example.corepayproductservice.prouduct.presentation.dto.req.ProductInfoUpdateReq;
import org.example.corepayproductservice.prouduct.presentation.dto.req.ProductUpdateAmountReq;
import org.example.corepayproductservice.prouduct.presentation.dto.req.ProductUpdateCategoryReq;
import org.example.corepayproductservice.prouduct.presentation.dto.res.ProductDto;

import java.util.List;

public interface ProductService {
    ProductDto creat(ProductCreatReq req);
    ProductDto updateInfo(Long id, ProductInfoUpdateReq req);
    boolean updateAmount(Long id, ProductUpdateAmountReq req);
    void updateCategory(Long id, ProductUpdateCategoryReq req);
    ProductDto get(Long id);
    List<ProductDto> getList();
    void delete(Long id);
}
