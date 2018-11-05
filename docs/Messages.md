# OrderMessage
## Customer requests a order processor of a bakery for some products to be delivered at specific time
### "<ddd.hh\> product_name:price; product_name:price; product_name:price"
<br />
# Status
## The order processor acknowledges the receipt of the order form the customer
###  "order_(received/declined)"
<br />
# requestForPendingOrders
## The production manager requests todays orders from the order processor
###  "todays_orders"
<br />
# todaysOrders
## The Order processor informs the production manager the list of orders due for today
###  "order_id;order_id;order_id;order_id"
<br />
# requestProductInfo
## The production manager requests the order for the list of products
### "product_list"
<br />
# orderInfo
## The order provides the list of all products to be fullfilled to the production manager
### "product_id:quantity:due_time;product_id:quantity:due_time;product_id:quantity:due_time"
<br />
# getTimeToDelivery
## The product manager requests the time to tranport an order
###  "time_to_transport:order_id"
<br />
# assignKneadingTask
## The production manager informs the kneading agent about the quantity of dough to be kneaded
### "knead:quantity"
<br />
# kneadingComplete
## The kneading agent  is ready for kneading
### "ready_to_knead;max_quantity"
<br />
# restingComplete
## The previously kneaded dough is ready for preparation
### "ready_to_pick"
<br />
# assignPrepTask
## Request the item preparation agents to prepare the items
### "item:quantity;item:quantity;item:quantity"
<br />
# preparationComplete
## The preparation agent informs the production manager that it is ready to prepare new items
### "ready_to_prepare"
<br />
# assignBakingTask
## The production manager requests the baking agent to bake the prepared items
### "bake:product_id,amount;product_id,amount;product_id,amount"
<br />
# BakingComplete
## The baking agent informs the production manager that baking is complete and oven is free
### "baking_done:number_of_empty_slots"
<br />
# itemsReadyToPack
## The baking agent informs the packaging agent that the items are cooled down and ready for packaging
### "item_id:quantity;item_id:quantity;item_id:quantity"
<br />
# itemsPacked
## The packing agent informs the Order tracker that some items have been packed
### "packed:product_id,number_of_boxex;product_id,number_of_boxex;product_id,number_of_boxex;"
<br />
# moveOrderAgent
## The production manager passes the responsibility of the order agent to the order tracker agent
### "order_id;order_id;order_id"
<br />
# updateCompletedProducts
## The order tracker informs the order agent that some of its products are ready for dispatch
### "ready:product_id,amount;product_id,amount;product_id,amount"
<br />
# OrderFulfilled
## The order agent informs the order tracker that the order is ready for delivery
### "order_fulfilled"
<br />
# GetDeliveryAddress
## The Order tracker request the order agent for the delivery address
### "delivery_address"
<br />
# DeliveryAddress
## The order informs the order tracker about the delivery address
### "<x_customer,y_customer>"
<br />
# RequestOrderTransport
## The order tracker requests transporation of a complete order
### "transport:order_id,<x_customer,y_customer>"
<br />
# queryTimeToDelivery
## The Transport agent requests the Street Network agent for time to move a truck between the bakery and the customer
### "x_start,y_start;x_end,y_end"
<br />
# deliverOrders
## The transport agent requests a truck to transport some orders
### "order_id:x_customer,y_customer;order_id:x_customer,y_customer;order_id:x_customer,y_customer"
<br />
# DeliveryStatus
## The truck informs the order that the products were delivered to the customer. The order can then die.
### "delivered_successfully"
<br />
