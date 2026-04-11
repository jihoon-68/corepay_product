package org.example.corepayproductservice.prouduct.application;

import org.example.corepayproductservice.prouduct.application.command.CreatedProductCommand;
import org.example.corepayproductservice.prouduct.application.command.UpdateAmountCommand;
import org.example.corepayproductservice.prouduct.application.command.UpdateCategoryCommand;
import org.example.corepayproductservice.prouduct.application.command.UpdateInfoCommand;
import org.example.corepayproductservice.prouduct.presentation.dto.req.ProductCreatReq;
import org.example.corepayproductservice.prouduct.presentation.dto.req.ProductInfoUpdateReq;
import org.example.corepayproductservice.prouduct.presentation.dto.req.ProductUpdateAmountReq;
import org.example.corepayproductservice.prouduct.presentation.dto.req.ProductUpdateCategoryReq;
import org.example.corepayproductservice.prouduct.presentation.dto.res.ProductDto;

import java.util.List;

public interface ProductService {
    ProductDto creat(CreatedProductCommand command);
    ProductDto updateInfo(UpdateInfoCommand command);
    boolean updateAmount(UpdateAmountCommand command);
    void updateCategory(UpdateCategoryCommand command);
    ProductDto get(Long id);
    List<ProductDto> getList();
    void delete(Long id);
}
