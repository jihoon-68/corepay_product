package org.example.corepayproductservice.product.application;

import org.example.corepayproductservice.product.application.command.CreatedProductCommand;
import org.example.corepayproductservice.product.application.command.UpdateAmountCommand;
import org.example.corepayproductservice.product.application.command.UpdateCategoryCommand;
import org.example.corepayproductservice.product.application.command.UpdateInfoCommand;
import org.example.corepayproductservice.product.presentation.dto.res.ProductDto;

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
