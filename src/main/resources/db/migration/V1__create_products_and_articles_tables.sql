CREATE TABLE products (
    id   BIGINT SERIAL NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE articles (
    id       BIGINT SERIAL NOT NULL PRIMARY KEY,
    name     VARCHAR(255) NOT NULL,
    in_stock INT NOT NULL DEFAULT 0,
    CHECK in_stock >= 0
);

CREATE TABLE products_articles (
    product_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    amount_of  INT NOT NULL,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (article_id) REFERENCES articles(id)
);