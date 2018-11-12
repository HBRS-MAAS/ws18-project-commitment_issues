# RequestQuotations (CFP)
## Customer requests for quotations from all the available bakeries for their list of products and a specific delivery date
### "<ddd.hh\> product_name:quantity; product_name:quantity; product_name:quantity"
<br>

# ReplyToCustomerRequest (PROPOSE/ REFUSE)
## The bakeries propose a quotation or refuse to accept an order
###  "Quotation" / "Rejected"
<br>

# PlaceOrder (ACCEPT_PROPOSAL)
## The Customer places the order with the bakery that provided the least quotation
###  "<ddd.hh\> product_name:quantity; product_name:quantity; product_name:quantity"
<br>

# ConfirmOrder (INFORM)
## The bakery informs the customer if the order was successfully placed.
###  "Accepted:OrderID" / "Rejected"
<br>

# PendingOrders (REQUEST)
## The Scheduler requests the OrderProcesor to provide a list of pending orders for the day.
### "RequestForOrders"
<br>

# TodaysOrders (INFORM)
## The OrderProcessor sends a list of order id's that are pending for today
### "OrderID;OrderID;OrderID"
<br>

# RequestProductInfo (REQUEST)
## The scheduler requests the orders to provide a list of needed products
###  "RequestForProductList"
<br>

# OrderInfo (INFORM)
## The Order provides a list of needed products to the Scheduler
### "product_name:quantity; product_name:quantity; product_name:quantity"
<br>

# GetTimeToDelivery (REQUEST)
## The OrderProcessor requests the StreetNetwork for the time needed to deliver the finished products to the customer
### "RequestTravelTime"
<br>

# TimeToDelivery (INFORM)
## The StreetNetwork informs the OrderProcessor about the tavel time from the bakery to the customer.
### "hh"
<br>

# FreeToKnead? (QUERY_IF)
## The Scheduler asks the Kneading agents if they are free to atke a kneading task
### "RequestToKnead"
<br>

# ReadyToKnead (CONFIRM)
## The Kneading agent confirms that it can take up a kneading task
### "AcceptToKnead"
<br>

# ProductToKnead (INFORM)
## The Scheduler informs the kneading agent about the product and its quantity to knead
### "product_id:quantity"
<br>

# FreeToPrepre? (QUERY_IF)
## The kneading agent asks the ItemPrep agents if they are free to prepare the products whose dough is ready after the resting time
### "RequestToPrepare"
<br>

# ReadyToPrep (CONFIRM)
## The ItemPrep agent confirms that it can accept a task to prepare the items
### "AcceptToPrepare"
<br>

# ProductToKnead (INFORM)
## The Kneading agent informs the ItemPrep agent about the products that must be prepared
### "product_id:quantity"
<br>

# ProofProducts (INFORM)
## The ItemPrep agent informs that proofer agent that the items have been prepared and are ready for baking.
### "order_id;order_id;order_id"
<br>

# updateCompletedProducts
## The order tracker informs the order agent that some of its products are ready for dispatch
### "product_id:quantity;product_id:quantity;product_id:quantity"
<br>

# OrderFulfilled
## The order agent informs the order tracker that the order is ready for delivery
### "order_fulfilled"
<br>

# GetDeliveryAddress
## The Order tracker request the order agent for the delivery address
### "delivery_address"
<br>

# DeliveryAddress
## The order informs the order tracker about the delivery address
### "x_customer,y_customer"
<br>

# RequestOrderTransport
## The order tracker requests transporation of a complete order
### "transport:order_id,<x_customer,y_customer>"
<br>

# queryTimeToDelivery
## The Transport agent requests the Street Network agent for time to move a truck between the bakery and the customer
### "x_start,y_start;x_end,y_end"
<br>

# deliverOrders
## The transport agent requests a truck to transport some orders
### "order_id:x_customer,y_customer;order_id:x_customer,y_customer;order_id:x_customer,y_customer"
<br>

# DeliveryStatus
## The truck informs the order that the products were delivered to the customer. The order can then die.
### "delivered_successfully"
<br>
