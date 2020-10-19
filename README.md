Warehouse
=========

### The Task

The assignment is to implement a warehouse software. This software should hold articles, and the articles should contain an identification number, a name and available stock. It should be possible to load articles into the software from a file, see the attached inventory.json.

The warehouse software should also have products, products are made of different articles. Products should have a name, price and a list of articles of which they are made from with a quantity. The products should also be loaded from a file, see the attached products.json.


The warehouse should have at least the following functionality;
* Get all products and quantity of each that is available with the current inventory
* Remove(Sell) a product and update the inventory accordingly

### The solution

Expose an interactive REST API to manipulate the products and inventory of the warehouse.

### The architecture

This application uses the [Hexagonal architecture](https://en.wikipedia.org/wiki/Hexagonal_architecture_(software)) concepts to create an onion architecture like structure, pushing the side effects operations to the outer layers (adapters) and keeping the domain just with pure functions and repository interfaces.

Additionally, it also embraces the functional programming in a Haskell-ish syntax with the IO monad for side effects and others monads across the codebase to reduce the boilerplate of complex functionality concisely. On the other hand, the application rely on the database ACID capabilities to ensure the correctness of the operations and avoid race-conditions.  

Finally, the goal is to have an application, as close as possible to a production ready app, focusing on the correct usage of different Thread polls strategies for non-blocking/asynchronous code and logging. Nonetheless, some important features are missing to be fully compliant to be production ready in order to keep the assignment small, such as, a better connection poll manager and, more importantly, metrics.

In the next section it's documented the API operations.

### The API

#### `GET /products/import`

Load products from JSON file asynchronously. Returns **202 Accepted** if a file is present and **400 BadRequest** if not.

```curl
$ curl -i -F 'data=@src/test/resources/inventory.json' http://localhost:8080/products/import
HTTP/1.1 202 Accepted
```

#### `GET /inventory/import` 

Load the inventory from JSON file asynchronously. Returns **202 Accepted** if a file is present and **400 BadRequest** if not.

```curl
$ curl -i -F 'data=@src/test/resources/inventory.json' http://localhost:8080/inventory/import
HTTP/1.1 202 Accepted
```

#### `GET /products/import` 

Load the products from JSON file asynchronously. Returns **202 Accepted** if a file is present and **400 BadRequest** if not.

```curl
$ curl -i -F 'data=@src/test/resources/products.json' http://localhost:8080/products/import
HTTP/1.1 202 Accepted
```

#### `GET /products`

Returns all products, it's articles and its availability in the stock.

```curl
$ curl http://localhost:8080/products | jq
{
  "products": [
    {
      "id": 1,
      "name": "Dining Chair",
      "is_available": true,
      "in_stock": 2,
      "contain_articles": [
        {
          "art_id": 1,
          "amount_of": 4,
          "in_stock": 12
        },
        {
          "art_id": 2,
          "amount_of": 8,
          "in_stock": 17
        },
        {
          "art_id": 3,
          "amount_of": 1,
          "in_stock": 2
        }
      ]
    },
    {
      "id": 2,
      "name": "Dinning Table",
      "is_available": true,
      "in_stock": 1,
      "contain_articles": [
        {
          "art_id": 1,
          "amount_of": 4,
          "in_stock": 12
        },
        {
          "art_id": 2,
          "amount_of": 8,
          "in_stock": 17
        },
        {
          "art_id": 4,
          "amount_of": 1,
          "in_stock": 1
        }
      ]
    }
  ]
}
```

#### `POST /products`

Insert a new product into the system. Returns **201 Created** with the `Location` header to the recently created product ID or **400 Bad Request** if the payload is invalid.

```bash
$ curl -i -XPOST http://localhost:8080/products -d @- << EOF
{
  "name": "BESTÅ",
  "contain_articles": [
    {"art_id": 2, "amount_of": 5}
  ]
}
EOF
HTTP/1.1 201 Created
Location: /products/3
```

#### `GET /products/:id`

Returns a product by its ID or **404 Not Found**.

```bash
$ curl http://localhost:8080/products/1 | jq
{
  "id": 1,
  "name": "Dining Chair",
  "is_available": true,
  "in_stock": 2,
  "contain_articles": [
    {
      "art_id": 1,
      "amount_of": 4,
      "in_stock": 12
    },
    {
      "art_id": 2,
      "amount_of": 8,
      "in_stock": 17
    },
    {
      "art_id": 3,
      "amount_of": 1,
      "in_stock": 2
    }
  ]
}
```

#### `PUT /products/:id`

Update the product's information. Returns **204 No Content** when successful, **400 Bad Request** if the payload is invalid or **404 Not Found**.

```bash
$ curl -i -XPUT http://localhost:8080/products/3 -d @- << EOF
{
  "name": "BESTÅ",
  "contain_articles": [
    {"art_id": 2, "amount_of": 4}
  ]
}
EOF
HTTP/1.1 204 No Content
```

#### `DELETE /products/:id`

Removes a product from the warehouse system. Returns **204 No Content** when successful or **404 Not Found**.

```bash
$ curl -i -XDELETE http://localhost:8080/products/3
HTTP/1.1 204 No Content
```

#### `POST /products/:id/buy`

Removes a product from the warehouse system. Returns **204 No Content** when successful, **400 Bad Request** if product is not available on stock or **404 Not Found**.

```bash
$ curl -i -XPOST http://localhost:8080/products/1/buy
HTTP/1.1 204 No Content
```