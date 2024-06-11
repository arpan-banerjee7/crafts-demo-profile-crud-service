# PROBLEM STATEMENT
A system supports below products for Small Business owners:
1. SmartBooks (FB) for Accounting
2. SB Payroll for paying small business employees
3. SB Payments for small business to receive payments from their customers
4. SSheets for time-tracking  
Customers can subscribe to any/all of the above products and by doing so, maintain a shared/common business profile across all of the products.
Example: Customer Ganesh runs a small flower shop and he has 4 employees in his shop. Ganesh has subscribed to all of the above products. Ganesh has a common business profile among all of these products.

A business profile is composed of below fields:
Field Name
Company Name Legal Name Business Address (Line1, Line 2, City, State, Zip, Country) Legal Address Tax Identifiers (PAN, EIN) Email Website

Customer can update their business profile from any of the above product workflows. Design and build a Business Profile service where every profile Create/Update request needs to be approved by all the subscribed products of the user.
In our use-case, for example, when Ganesh updates his company address via API, all of the above products (#1, #2, #3, #4) have to validate the data before data can be saved in the database.
Assume each of the above products has exposed a REST API that can be invoked to validate the data. For the purpose of implementation, you can use a dummy Validate API and use the same API for mocking each of the products.

Considerations
1. New products get added every 3 months once
2. The load on this to-be-built system will be high (1000 tps)
3. Response time of CRUD operation should be < 100ms  
 
Design the profile validation system (HLD)
* Consider using the “Store and Validate” approach. Ie., Accept the request and  return the response. Do validation with all products behind the scenes  asynchronously.  
* Have an API to identify which state the validation is in at any point in time.  Like In_Progress, Rejected, Accepted, and so on. 
* Build a spring boot microservice with REST/GraphQL API’s for CRUD operation Have Well-defined contract Have well-defined HTTP status code and error messages. Use Dynamo or equivalent database for persistence (Well thought Schema design) Use the Cache layer for achieving scalability, resiliency, and performance as needed.


Aim for:
1. Loose coupling between each system that needs to validate the data.
2. Use the Spring framework as much as possible.
3. Focus on resiliency scenarios as well (And not Happy Path alone)
4. Apply design patterns as much as possible (Like Hystrix for circuit-breaker, Factory  pattern, and so on)
5. Try following TDD or have good coverage for Unit Tests.
6. As a surprise, you can also build UI experience in React to update the profile fields.
7. List out the Non-Functional areas that you would consider to deploy this application  to Prod and how do you suggest to achieve the same.

## Dependent Microservices
**Backend**
- [crafts-demo-profile-crud-service](https://github.com/arpan-banerjee7/crafts-demo-profile-crud-service)
- [crafts-demo-consumer](https://github.com/arpan-banerjee7/crafts-demo-consumer)
- [crafts_demo_config](https://github.com/arpan-banerjee7/crafts_demo_config)
- [crafts_demo_config_server](https://github.com/arpan-banerjee7/crafts_demo_config_server)

**Frontend**
- [crafts-demo-user-profile-UI](https://github.com/arpan-banerjee7/crafts-demo-user-profile-UI)

**Supporting Services** (Mimicks 3rd party apis)
- [crafts_demo_product_val_1](https://github.com/arpan-banerjee7/crafts_demo_product_val_1)
- [crafts_demo_product_val_2](https://github.com/arpan-banerjee7/crafts_demo_product_val_2)
- [crafts_demo_product_val_3](https://github.com/arpan-banerjee7/crafts_demo_product_val_3)
- [crafts_demo_product_val_4](https://github.com/arpan-banerjee7/crafts_demo_product_val_4)


# HLD
![user-profile-validation-system_HLD (1)](https://github.com/arpan-banerjee7/crafts-demo-profile-crud-service/assets/62155359/a34b2940-8e37-4ba7-b992-e926ac887567)

# UI Screenshots
![image](https://github.com/arpan-banerjee7/crafts-demo-profile-crud-service/assets/62155359/3c0ed446-9297-4f41-9fea-d7ae2448ecfc)

![image](https://github.com/arpan-banerjee7/crafts-demo-profile-crud-service/assets/62155359/21e78d0a-76b4-4d8a-bd6c-f2754ce3d033)

![image](https://github.com/arpan-banerjee7/crafts-demo-profile-crud-service/assets/62155359/79ad8f33-bdcb-4102-926d-c0f1cde3667d)




