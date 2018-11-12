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
### "Bakery:<x_pos, y_pos>;Customer:<x_pos, y_pos>"
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

# ProductsReadyToBake (INFORM)
## The Proofer agent informs the baking scheduler that a batch of items is ready to be baked
### "product_id:quantity;product_id:quantity;product_id:quantity"
<br>

# FreeToPrepre? (QUERY_IF)
## The BakingScheduler asks the baking agents if they have any free ovens to bake some products
### "RequestToBake"
<br>

# NumberOfFreeTrays (INFORM)
## The baking agents inform the BakingSheduler if they have any free ovens.
### "NumOfFreeOvens"
<br>

# ProductsToBake (INFORM)
## The BakingScheduler informs the baking agent to bake a batch of products
### "product_id:quantity;product_id:quantity;product_id:quantity"
<br>

# CoolProducts (INFORM)
## The BakingAgent informs the clooing agent that a batch of products has been baked and must be cooled down
### "product_id:quantity;product_id:quantity;product_id:quantity"
<br>

# ProductsReadyToPack (INFORM)
## The cooling agent informs the packing scheduler that some items have been cooled and are ready to be packed
### "product_id:quantity;product_id:quantity;product_id:quantity"
<br>

# FreeToDecorate? (QUERY_IF)
## The packing scheduler asks the decorator agents if they are free to start decoration of produced products
### "RequestToDecorate"
<br>

# ReadyToDecorate (CONFIRM)
## The decorator agent informs the paking scheduler that it can pick up a task fr deocrating items.
### "AcceptToDecorate"
<br>

# ProductsToDecorate (INFORM)
## The PackingScheduler informs the DecoratorAgent about the list of products to be decorated
### "product_id:quantity"
<br>

# FreeToPack? (QUERY_IF)
## The DecoratorAgent asks the packing agents if they are free to start a new packing task
### "RequestToPack"
<br>

# ReadyToPack (CONFIRM)
## The packing agent informs the decorator agent that it can pick up a task fr packing items.
### "AcceptToPack"
<br>

# PackProducts (INFORM)
## The Decorator agent provides a list of items to be packed
### "product_id:quantity"
<br>

# BoxesToDeliver (INFORM)
## The PackingAgent informs the loadingBay agent that some items have been packed in boxes.
### "BoxID:product_id,quantity;BoxID:product_id,quantity;BoxID:product_id,quantity"
<br>

# BoxesReadyToDeliver (INFORM)
## The LoadingBay informs the OrderTracker that some items have been packed in boxes.
### "BoxID:product_id,quantity;BoxID:product_id,quantity;BoxID:product_id,quantity"
<br>

# OrderProgress (INFORM)
## The order tracker informs the orders about some products needed by them are ready to be dispatched
### "product_id:quantity;product_id:quantity;product_id:quantity;"
<br>

# OrderFulfilled (CONFIRM)
## If all the products needed by an order are ready, the order informs the OrderTracker that the order is now ready for dispatch
### "AllItemsReady"
<br>

# RequestOrderTransport (REQUEST)
## The OrderTracker informs the Transport agent that an order is ready to be delivered.
### "OrderID;OrderID;OrderID;"
<br>

# RequestTimeForDelivery (CFP)
## The TransportAgent seend out a message to all the trucks to request their best time to deliver a product from the bakery to a customer location.
### "Bakery:<x_pos, y_pos>;Customer:<x_pos, y_pos>"
<br>

# queryTimeToDelivery (INFORM)
## The Trucks check with the Street network about time to travel multiple different distances (E.g. current_pos to bakery and bakery to customer)
### "Pos1:<x_pos, y_pos>;Pos2:<x_pos, y_pos>"
<br>

# TimeToDelivery (INFORM)
## The StreetNetwork informs the Trucks about the tavel time from the bakery to the customer.
### "hh"
<br>

# ReplyToRequest (PROPOSE / REFUSE)
## The Trucks inform the Transport agent about their best times to deliver the orders
### "product_id:quantity"
<br>

# AssignDelivery (ACCEPT_PROPOSAL)
## The Delivery agent contracts the truck with the best time to delivery to deliver an order
### "OrderID"
<br>

# ConfirmAssignment (INFORM)
## The Trucks inform the trasport about if they accepted or rejected the contract.
### "Acceptance:OrderID" / "Rejection:OrderID"
<br>

# OrderSuccessfullyDelivered (INFORM)
## The trucks post a message into the mailbox after an order has been delivered
### "Delivered:OrderID"
<br>


