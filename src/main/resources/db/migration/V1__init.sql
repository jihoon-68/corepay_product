-- 2. 제품 (Product) 테이블
CREATE TABLE product (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         name VARCHAR(100) NOT NULL,
                         price INT NOT NULL,
                         discount INT NOT NULL DEFAULT 0,
                         amount INT NOT NULL DEFAULT 0,
                         category ENUM('FOOD', 'SHOES', 'ELECTRONICS', 'ETC') NOT NULL DEFAULT 'ETC',
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;